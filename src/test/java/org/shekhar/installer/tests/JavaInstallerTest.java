package org.shekhar.installer.tests;


import com.xebialabs.overcast.OverthereUtil;
import com.xebialabs.overcast.host.CloudHost;
import com.xebialabs.overcast.host.CloudHostFactory;
import com.xebialabs.overthere.CmdLine;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereExecutionOutputHandler;
import com.xebialabs.overthere.local.LocalConnection;
import com.xebialabs.overthere.util.CapturingOverthereExecutionOutputHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Arrays;

import static com.xebialabs.overcast.OvercastProperties.getOvercastProperty;
import static com.xebialabs.overcast.OverthereUtil.overthereConnectionFromURI;
import static com.xebialabs.overthere.util.CapturingOverthereExecutionOutputHandler.capturingHandler;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class JavaInstallerTest {

    private static final String JDK_FILE_SOURCE = "jdk";
    private static final String JDK_DESTINATION_PATH = "c:\\";
    private static final String JDK_EXE_PATH = JDK_DESTINATION_PATH + "jdk-7u75-windows-x64.exe";
    public static final String VAGRANT_HOST = "vagrantHost";
    private static CloudHost vagrantHost;

    private static final Logger logger = LoggerFactory.getLogger(JavaInstallerTest.class);

    @BeforeClass
    public static void setup() {
        vagrantHost = CloudHostFactory.getCloudHost(VAGRANT_HOST);
        vagrantHost.setup();
    }

    @AfterClass
    public static void teardown() throws InterruptedException {
        Thread.sleep(150000L);
        vagrantHost.teardown();
    }


    @Test
    public void shouldInstallJDK7() throws Exception {
        assertThat(vagrantHost, is(notNullValue()));
        String url = makeWinrmConnectionUrl(VAGRANT_HOST);
        String hostName = vagrantHost.getHostName();
        OverthereConnection dest = getOverthereConnection(url, hostName);

        copyFiles(LocalConnection.getLocalConnection(), dest);

        CmdLine jdkInstallCommand = new CmdLine().addRaw(String.format("%s /quiet /li", JDK_EXE_PATH));
        CapturingOverthereExecutionOutputHandler stdOutCapture = capturingHandler();
        CapturingOverthereExecutionOutputHandler stdErrCapture = capturingHandler();

        OverthereExecutionOutputHandler stdOutHandler = stdOutCapture;
        OverthereExecutionOutputHandler stdErrHandler = stdErrCapture;

        int exitCode = dest.execute(stdOutHandler, stdErrHandler, jdkInstallCommand);
        assertThat(exitCode, is(equalTo(0)));

        CmdLine setJavaInPath = new CmdLine().addRaw("set PATH=\"c:\\Program Files\\Java\\jdk1.7.0_75\\bin\";%PATH%");
        exitCode = dest.execute(stdOutHandler, stdErrHandler, setJavaInPath);
        assertThat(exitCode, is(equalTo(0)));

//        CmdLine javaCmd = new CmdLine().addRaw("java -version");
//        exitCode = dest.execute(stdOutHandler, stdErrHandler, javaCmd);
//        assertThat(exitCode, is(equalTo(0)));

    }

    private String makeWinrmConnectionUrl(String testTarget) throws UnsupportedEncodingException {
        String username = getOvercastProperty(String.format("%s.testcfg.username", testTarget), "Administrator");
        String password = getOvercastProperty(String.format("%s.testcfg.password", testTarget), "vagrant");
        String connectionTimeoutMillis = getOvercastProperty("installer.test.connectionTimeoutMillis", "8000");
        String winRmTimeout = getOvercastProperty("installer.test.winrmTimeout", "PT600.000S");

        return String.format("cifs://%s@{0}?os=WINDOWS&connectionType=WINRM_INTERNAL&connectionTimeoutMillis=%s&winrmTimeout=%s&password=%s",
                URLEncoder.encode(username, "UTF-8"),
                URLEncoder.encode(connectionTimeoutMillis, "UTF-8"),
                URLEncoder.encode(winRmTimeout, "UTF-8"),
                URLEncoder.encode(password, "UTF-8")
        );
    }

    private void copyFiles(OverthereConnection src, OverthereConnection dest) {
        OverthereUtil.copyFiles(src, dest, Arrays.asList(JDK_FILE_SOURCE, JDK_DESTINATION_PATH));
    }

    private OverthereConnection getOverthereConnection(String url, String ip) {
        String finalUrl = MessageFormat.format(url, ip);
        OverthereConnection connection = overthereConnectionFromURI(finalUrl);
        return connection;
    }
}
