package io.dockstore.webservice;

import java.io.File;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import io.dockstore.client.cli.BaseIT;
import io.dockstore.client.cli.Client;
import io.dockstore.common.CommonTestUtilities;
import io.dockstore.common.DescriptorLanguage;
import io.dockstore.common.NonConfidentialTest;
import io.dockstore.common.SourceControl;
import io.dockstore.common.TestingPostgres;
import io.dockstore.webservice.core.BioWorkflow;
import io.dockstore.webservice.core.Token;
import io.dockstore.webservice.core.TokenType;
import io.dockstore.webservice.jdbi.TokenDAO;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.client.proxy.ProxyConfiguration;
import io.dropwizard.client.ssl.TlsConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import io.swagger.client.ApiClient;
import io.swagger.client.api.HostedApi;
import io.swagger.client.api.WorkflowsApi;
import io.swagger.client.model.SourceFile;
import io.swagger.client.model.Workflow;
import io.swagger.client.model.WorkflowVersion;
import io.swagger.model.DescriptorType;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

import static io.dockstore.common.CommonTestUtilities.getWebClient;
import static io.dockstore.common.Hoverfly.SUFFIX1;
import static io.dockstore.common.Hoverfly.ZENODO_SIMULATION_SOURCE;
import static io.dockstore.common.Hoverfly.getFakeCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(NonConfidentialTest.class)
public class ZenodoResourceIT {

    private static final String DROPWIZARD_CONFIGURATION_FILE_PATH = CommonTestUtilities.PUBLIC_CONFIG_PATH;
    //public static final String LOCAL_TRUST_STORE_PATH = ResourceHelpers.resourceFilePath("LocalTrustStore");


    // public static final DropwizardTestSupport<DockstoreWebserviceConfiguration> createDropWizardTestSupport() {
    //
    //     HttpClientConfiguration zenodoHttpClientConfiguration = new HttpClientConfiguration();
    //
    //     TlsConfiguration zenodoTlsConfiguration = new TlsConfiguration();
    //     File tlsTrustStoreFile = new File(LOCAL_TRUST_STORE_PATH);
    //     zenodoTlsConfiguration.setTrustStorePath(tlsTrustStoreFile);
    //     zenodoTlsConfiguration.setTrustStorePassword("changeit");
    //
    //     zenodoHttpClientConfiguration.setTlsConfiguration(zenodoTlsConfiguration);
    //
    //     DockstoreWebserviceConfiguration zenodoDockstoreWebserviceConfiguration = new DockstoreWebserviceConfiguration();
    //     zenodoDockstoreWebserviceConfiguration.setHttpClientConfiguration(zenodoHttpClientConfiguration);
    //     zenodoDockstoreWebserviceConfiguration.setZenodoClientID(getFakeCode(SUFFIX1));
    //     zenodoDockstoreWebserviceConfiguration.setZenodoClientSecret(getFakeCode(SUFFIX1));
    //     zenodoDockstoreWebserviceConfiguration.setDatabase;
    //
    //     return new DropwizardTestSupport<>(DockstoreWebserviceApplication.class, zenodoDockstoreWebserviceConfiguration);
    //
    // };

    public static final DropwizardTestSupport<DockstoreWebserviceConfiguration> SUPPORT = new DropwizardTestSupport<>(
            DockstoreWebserviceApplication.class, DROPWIZARD_CONFIGURATION_FILE_PATH, ConfigOverride.config("zenodoClientID", getFakeCode(SUFFIX1)),
            ConfigOverride.config("zenodoClientSecret", getFakeCode(SUFFIX1)));


    //public static final DropwizardTestSupport<DockstoreWebserviceConfiguration> SUPPORT = createDropWizardTestSupport();


    public static final long ZENODO_USER1_ID = 1L;
    private static TestingPostgres testingPostgres;
    @Rule
    public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // These are not from Hoverfly, it's actually in the starting database
    public final static String ZENODO_ACCOUNT_USERNAME_1 = "admin@admin.com";
    //public final static String ZENODO_ACCOUNT_USERNAME_1 = "potato";

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog().muteForSuccessfulTests();

    @ClassRule
    public static final HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(ZENODO_SIMULATION_SOURCE);


    @BeforeClass
    public static void dropAndRecreateDB() throws Exception {
        CommonTestUtilities.dropAndRecreateNoTestData(SUPPORT, DROPWIZARD_CONFIGURATION_FILE_PATH);
        SUPPORT.before();
        testingPostgres = new TestingPostgres(SUPPORT);
    }

    @AfterClass
    public static void afterClass(){
        SUPPORT.after();
    }

    @After
    public void after() throws InterruptedException {
        BaseIT.assertNoMetricsLeaks(SUPPORT);
    }

    @Before
    public void setup() throws Exception {
        CommonTestUtilities.dropAndCreateWithTestWorkflowAndWorkflowVersion(SUPPORT, false, DROPWIZARD_CONFIGURATION_FILE_PATH);
        DockstoreWebserviceApplication application = SUPPORT.getApplication();
        SessionFactory sessionFactory = application.getHibernate().getSessionFactory();
        TokenDAO tokenDAO = new TokenDAO(sessionFactory);
        // non-confidential test database sequences seem messed up and need to be iterated past, but other tests may depend on ids
        testingPostgres.runUpdateStatement("alter sequence enduser_id_seq increment by 50 restart with 100");
        testingPostgres.runUpdateStatement("alter sequence token_id_seq increment by 50 restart with 100");
        // used to allow us to use tokenDAO outside of the web service
        Session session = application.getHibernate().getSessionFactory().openSession();
        ManagedSessionContext.bind(session);
        final Transaction transaction = session.beginTransaction();
        tokenDAO.create(createToken(SUFFIX1, ZENODO_ACCOUNT_USERNAME_1, ZENODO_USER1_ID));
        transaction.commit();
        session.close();
    }

    private Token createToken(String token, String username, long id) {
        final Token fakeZenodoToken = new Token();
        fakeZenodoToken.setTokenSource(TokenType.ZENODO_ORG);
        fakeZenodoToken.setContent(token);
        fakeZenodoToken.setUsername(username);
        fakeZenodoToken.setUserId(id);
        return fakeZenodoToken;
    }

    @Test
    public void refreshWithAppInstalledOnOrg() {
        // final io.dockstore.webservice.core.Workflow workflow = new BioWorkflow();
        // workflow.setSourceControl(SourceControl.GITHUB);
        // workflow.setOrganization("DataBiosphere");
        // workflow.setRepository("topmed-workflows");
        // workflow.setWorkflowName("UM_variant_caller_wdl");
        // workflow.setDescriptorType(DescriptorLanguage.WDL);
        // workflow.

        // final io.swagger.client.model.WorkflowVersion zenodoWorkflowVersion = new io.swagger.client.model.WorkflowVersion();
        // zenodoWorkflowVersion.setWorkflowPath("zenodo_test.wdl");
        // zenodoWorkflowVersion.setName("1.0.0");
        // zenodoWorkflowVersion.setFrozen(true);
        // zenodoWorkflowVersion.setName("master");
        // zenodoWorkflowVersion.setValid(true);
        // Date earlierDate = new Date(100L);
        // zenodoWorkflowVersion.setLastModified(earlierDate);
        // //Long workflowVersionId = zenodoWorkflowVersion.getId();
        //
        // List<WorkflowVersion> listWorkflowVersions = new ArrayList<>();
        // listWorkflowVersions.add(zenodoWorkflowVersion);
        //
        //
        //
        // //final WorkflowsApi workflowsApi  = new WorkflowsApi(getWebClient(true, ZENODO_ACCOUNT_USERNAME_1, testingPostgres));
        final WorkflowsApi workflowsApi  = new WorkflowsApi(getWebClient(true, ZENODO_ACCOUNT_USERNAME_1, testingPostgres));

        // //io.swagger.client.model.Workflow zenodo_workflow = workflowsApi.manualRegister(SourceControl.GITHUB.getFriendlyName(), ZENODO_WORKFLOW_REPO,
        //   //      "/zenodo_test.wdl", "", "wdl", "/zenodo_test.wdl.json");
        //
        //
        // //zenodo_workflow.setWorkflowVersions(listWorkflowVersions);
        // //Long workflowId = zenodo_workflow.getId();
        //
        // // Make sure no workflow version has a DOI at the beginning of the test
        Long workflowId = 32L;
        io.swagger.client.model.Workflow initialWorkflow = workflowsApi.getWorkflow(workflowId, null);
        //java.util.List<io.swagger.client.model.Workflow> initialWorkflows = workflowsApi.getAllWorkflowByPath("testWorkflow");

        // initialWorkflow.setWorkflowVersions(listWorkflowVersions);
        // initialWorkflow.setLastModified(1);
        // workflowsApi
        //
        //
        java.util.List<io.swagger.client.model.WorkflowVersion> initialWorkflowVersions = initialWorkflow.getWorkflowVersions();
        // // final Optional<io.swagger.client.model.WorkflowVersion> initialWorkflowVersionWithDOI = initialWorkflowVersions.stream()
        // //         .filter(w -> StringUtils.isNotEmpty(w.getDoiURL())).findFirst();
        // // assertEquals(0, initialWorkflowVersionWithDOI.isPresent());
        int numworkflowversions = initialWorkflowVersions.size();
        // //initialWorkflow.setWorkflowVersions();
        //
        WorkflowVersion zenodoWorkflowVersionAdded = initialWorkflowVersions.get(0);
        // Long workflowVersionId = zenodoWorkflowVersionAdded.getId();





        //final WorkflowsApi workflowsApi  = new WorkflowsApi(getWebClient(true, ZENODO_ACCOUNT_USERNAME_1, testingPostgres));

        final ApiClient webClient = getWebClient(true, ZENODO_ACCOUNT_USERNAME_1, testingPostgres);

        HostedApi hostedApi = new HostedApi(webClient);
        Workflow hostedWorkflow = hostedApi.createHostedWorkflow("name", null, DescriptorType.CWL.toString(), null, null);
        assertNotNull(hostedWorkflow.getLastModifiedDate());
        assertNotNull(hostedWorkflow.getLastUpdated());

        // make a couple garbage edits
        SourceFile source = new SourceFile();
        source.setPath("/Dockstore.cwl");
        source.setAbsolutePath("/Dockstore.cwl");
        source.setContent("cwlVersion: v1.0\nclass: Workflow");
        source.setType(SourceFile.TypeEnum.DOCKSTORE_CWL);

        final Workflow updatedHostedWorkflow = hostedApi.editHostedWorkflow(hostedWorkflow.getId(), Lists
                .newArrayList(source));
        assertNotNull(updatedHostedWorkflow.getLastModifiedDate());
        assertNotNull(updatedHostedWorkflow.getLastUpdated());

        // Create a DOI for one of the workflow versions
        Long hostedWorkflowId = updatedHostedWorkflow.getId();
        List<WorkflowVersion> listWorkflowVersions = updatedHostedWorkflow.getWorkflowVersions();


        WorkflowVersion workflowVersion = listWorkflowVersions.get(0);
        workflowVersion.setFrozen(true);

        Long workflowVersionId = workflowVersion.getId();

        List<WorkflowVersion> workflowVersions = workflowsApi.updateWorkflowVersion(updatedHostedWorkflow.getId(), Lists.newArrayList(workflowVersion));

        java.util.List<io.swagger.client.model.WorkflowVersion> doiWorkflowVersions =
                workflowsApi.requestDOIForWorkflowVersion(updatedHostedWorkflow.getId(), workflowVersionId, "");

        // Check to make sure the workflow version now has a DOI
        final Optional<io.swagger.client.model.WorkflowVersion> workflowVersionWithDOI = doiWorkflowVersions.stream()
                .filter(w -> StringUtils.isNotEmpty(w.getDoiURL())).findFirst();
        assertEquals(1, workflowVersionWithDOI.isPresent());

    }

}

