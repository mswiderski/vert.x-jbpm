package org.jbpm.vertx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.kie.services.impl.KModuleDeploymentService;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
import org.jbpm.kie.services.impl.ProcessServiceImpl;
import org.jbpm.kie.services.impl.RuntimeDataServiceImpl;
import org.jbpm.kie.services.impl.UserTaskServiceImpl;
import org.jbpm.kie.services.impl.bpmn2.BPMN2DataServiceImpl;
import org.jbpm.runtime.manager.impl.RuntimeManagerFactoryImpl;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorImpl;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorManager;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorMerger;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ListenerSupport;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.ProcessDefinition;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.jbpm.vertx.listeners.RegisterHandlerDeploymentListener;
import org.jbpm.vertx.security.VertxIdentityProvider;
import org.kie.internal.query.QueryContext;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.conf.MergeMode;
import org.kie.internal.runtime.conf.NamedObjectModel;
import org.kie.internal.runtime.conf.RuntimeStrategy;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/*
 */
public class JbpmVerticle extends Verticle {

	private PoolingDataSource ds;
	
	protected EntityManagerFactory emf;
	protected DeploymentService deploymentService;    
	protected DefinitionService bpmn2Service;
	protected RuntimeDataService runtimeDataService;
	protected ProcessService processService;
	protected UserTaskService userTaskService;
	
	protected RegisterHandlerDeploymentListener listener;
	protected VerticleServiceProvider provider;

	public void start() {
	    
        
	    JsonObject config = container.config();	    	    
	    final String containerToStart = config.getString("containerId");
		if (containerToStart == null || containerToStart.isEmpty()) {
		    container.logger().error("No container info given, exiting");
		    container.exit();
		    return;
		}
		provider = VerticleServiceProvider.configure(vertx, container);
		listener = new RegisterHandlerDeploymentListener(vertx, provider);
        configureServices(containerToStart, config.getBoolean("managed"));
		
		try {
		    String[] gav = containerToStart.split(":");
		    
			KModuleDeploymentUnit unit = new KModuleDeploymentUnit(gav[0], gav[1], gav[2]);
			unit.setStrategy(RuntimeStrategy.PER_PROCESS_INSTANCE);
			unit.setDeploymentDescriptor(getDescriptor());
			
			deploymentService.deploy(unit);
			
			container.logger().info("Container " + containerToStart + " deployed and running");
		} catch (Exception e) {
		    container.logger().error("Error when deploying container " + containerToStart, e);
		}
		
		vertx.eventBus().registerHandler("jbpm-endpoint",
				new Handler<Message<JsonObject>>() {
					@Override
					public void handle(Message<JsonObject> message) {
						JsonObject data = new JsonObject();
						data.putString("containerId", containerToStart);
						Collection<ProcessDefinition> processes = runtimeDataService.getProcesses(new QueryContext(0, 100));
						JsonArray array = new JsonArray();
						for (ProcessDefinition process : processes) {
							array.addObject(new JsonObject("{\"" + process.getId() + "\":\"" + process.getName() + "\"}"));
						}
						data.putArray("processes", array);
						vertx.eventBus().publish("jbpm-processes", data);
					}
				});

		container.logger().info("jBPM verticle started");

	}
	
	protected DeploymentDescriptor getDescriptor() {
	    DeploymentDescriptorManager descriptorManager = new DeploymentDescriptorManager("org.jbpm.domain");
	    
	    DeploymentDescriptor defaultDescriptor = descriptorManager.getDefaultDescriptor();
	    
	    DeploymentDescriptor descriptor = new DeploymentDescriptorImpl("org.jbpm.domain");
        
        descriptor.getBuilder()
        .addWorkItemHandler(new NamedObjectModel("mvel", "Send Task", "new org.jbpm.vertx.handlers.VertxSendTaskWorkItemHandler()"))
        .addWorkItemHandler(new NamedObjectModel("mvel", "Receive Task", "new org.jbpm.vertx.handlers.VertxReceiveTaskWorkItemHandler(runtimeManager)"));
	    
	    return new DeploymentDescriptorMerger().merge(defaultDescriptor, descriptor, MergeMode.MERGE_COLLECTIONS); 
	}

	@Override
	public void stop() {
		super.stop();
		
		emf.close();
		if (ds != null) {
    		ds.close();
    	}		
	}



	protected void buildDatasource(String containerId, boolean managed) {
	    
	    TransactionManagerServices.getConfiguration()
	    .setLogPart1Filename(containerId + "-btm1.tlog")
	    .setLogPart2Filename(containerId + "-btm2.tlog")
	    .setServerId(containerId + "@" + TransactionManagerServices.getConfiguration().getServerId());
	    
		ds = new PoolingDataSource();
		ds.setUniqueName("jdbc/jbpm");

		if (!managed) {
    		ds.setClassName("org.h2.jdbcx.JdbcDataSource");
    		ds.setMaxPoolSize(15);
    		ds.setAllowLocalTransactions(true);
    		ds.getDriverProperties().put("user", "sa");
    		ds.getDriverProperties().put("password", "sasa");
    		ds.getDriverProperties().put("URL", "jdbc:h2:mem:mydb");
		} else {
		    ds.setClassName("org.postgresql.xa.PGXADataSource");
            ds.setMaxPoolSize(15);
            ds.setAllowLocalTransactions(true);
            ds.getDriverProperties().put("user", "jbpm");
            ds.getDriverProperties().put("password", "jbpm");
            ds.getDriverProperties().put("serverName", "localhost");
            ds.getDriverProperties().put("portNumber", "5432");
            ds.getDriverProperties().put("databaseName", "jbpm");
		}
		ds.init();
	}
	
	protected void configureServices(String containerId, boolean managed) {
		buildDatasource(containerId, managed);
		Map<String, String> props = new HashMap<String, String>();
		if (managed) {
		    props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
		}
		emf = Persistence.createEntityManagerFactory("org.jbpm.domain", props);
		EntityManagerFactoryManager.get().addEntityManagerFactory("org.jbpm.domain", emf);
		
		// build definition service
		bpmn2Service = new BPMN2DataServiceImpl(); 
		provider.addService("definition", bpmn2Service);
		
		// build deployment service
		deploymentService = new KModuleDeploymentService();
		((KModuleDeploymentService)deploymentService).setBpmn2Service(bpmn2Service);
		((KModuleDeploymentService)deploymentService).setEmf(emf);
		((KModuleDeploymentService)deploymentService).setIdentityProvider(new VertxIdentityProvider());
		((KModuleDeploymentService)deploymentService).setManagerFactory(new RuntimeManagerFactoryImpl());		
		provider.addService("deployment", deploymentService);
		
		// build runtime data service
		runtimeDataService = new RuntimeDataServiceImpl();
		((RuntimeDataServiceImpl) runtimeDataService).setCommandService(new TransactionalCommandService(emf));
		((RuntimeDataServiceImpl) runtimeDataService).setIdentityProvider(new VertxIdentityProvider());
		((RuntimeDataServiceImpl) runtimeDataService).setTaskService(HumanTaskServiceFactory.newTaskServiceConfigurator().entityManagerFactory(emf).getTaskService());
		((KModuleDeploymentService)deploymentService).setRuntimeDataService(runtimeDataService);
		provider.addService("runtime", runtimeDataService);
		
		// set runtime data service as listener on deployment service
		((KModuleDeploymentService)deploymentService).addListener(((RuntimeDataServiceImpl) runtimeDataService));
		
		// build process service
		processService = new ProcessServiceImpl();
		((ProcessServiceImpl) processService).setDataService(runtimeDataService);
		((ProcessServiceImpl) processService).setDeploymentService(deploymentService);
		provider.addService("process", processService);
		
		// build user task service
		userTaskService = new UserTaskServiceImpl();
		((UserTaskServiceImpl) userTaskService).setDataService(runtimeDataService);
		((UserTaskServiceImpl) userTaskService).setDeploymentService(deploymentService);
		provider.addService("task", userTaskService);
		
		if (deploymentService instanceof ListenerSupport) {
			
			((ListenerSupport) deploymentService).addListener(listener);
		}
	}
}
