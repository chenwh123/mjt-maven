package com.chen.test.sshd;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.AbstractCommandSupport;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.shell.ShellFactory;
import org.apache.sshd.server.subsystem.SubsystemFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSHTestServer {
    public int getSSHPort(){
        return sshd.getPort();
    }

    public String getVirtualFileSystemPath() {
        return virtualFileSystemPath;
    }

    public void setVirtualFileSystemPath(String virtualFileSystemPath) {
        this.virtualFileSystemPath = virtualFileSystemPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private static SshServer sshd;
    private String virtualFileSystemPath = "target/ssh_vfs/";

    private String username = "nifiuser";
    private String password = "nifipassword";

    public void SSHTestServer(){

    }

    public void startServer() throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setHost("localhost");
        sshd.setPort(22);

        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

        //Accept all keys for authentication
        sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);

        //Allow username/password authentication using pre-defined credentials
        sshd.setPasswordAuthenticator((username, password, serverSession) ->  true);

        sshd.setShellFactory(new ShellFactory() {
            @Override
            public Command createShell(ChannelSession channel) throws IOException {
                return new AbstractCommandSupport(null, null) {
                    @SneakyThrows
                    @Override
                    public void run() {
                        String command = getCommand();
                        if (command == null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream()));
                            while ((command = reader.readLine()) != null) {
                                handleCmd(command);
                            }
                        }else{
                            handleCmd(command);
                        }
                    }

                    public void handleCmd(String cmd) throws Exception{
                        System.out.println("receive cmd = " + cmd);
                        getOutputStream().write(cmd.getBytes(StandardCharsets.UTF_8));
                        getOutputStream().flush();
                    }
                };
            }
        });

        sshd.start();
    }

    public static void main(String[] args) throws Exception {
        new SSHTestServer().startServer();
        System.out.println("server started");
        TimeUnit.SECONDS.sleep(10000);
    }

}