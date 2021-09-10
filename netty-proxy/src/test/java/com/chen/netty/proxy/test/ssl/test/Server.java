package com.chen.netty.proxy.test.ssl.test;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author chenwh
 * @date 2021/9/10
 */
@Slf4j
public class Server {
    public static String SERVER_KEY_STORE_PASSWORD = "123456";
    public static String SERVER_TRUST_KEY_STORE_PASSWORD = "123456";
    public static Integer DEFAULT_PORT = 8088;

    @Test
    public void check() throws Exception {
        KeyStore jks = KeyStore.getInstance("JKS");
        jks.load(getStream("/kserver.keystore"), "123456".toCharArray());
        Enumeration<String> aliases = jks.aliases();
        String aliase = null;
        while (aliases.hasMoreElements()) {
            aliase = aliases.nextElement();
        }
        //证书
        Certificate certificate = jks.getCertificate(aliase);
        //公钥
        PublicKey publicKey = certificate.getPublicKey();
        //私钥
        PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) jks.getEntry(aliase, new KeyStore.PasswordProtection("123456".toCharArray()))).getPrivateKey();
        System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        String sign = Base64.getEncoder().encodeToString(
                sign("测试msg".getBytes(),
                        privateKey,
                        "SHA1withRSA",
                        null));
        boolean verfi = verify("测试msg".getBytes(),Base64.getDecoder().decode(sign), publicKey,"SHA1withRSA",null);
        System.out.println(verfi);
    }

    /**
     * 签名
     */
    public static byte[] sign(byte[] message, PrivateKey privateKey, String algorithm, String provider) throws Exception {
        Signature signature;
        if (null == provider || provider.length() == 0) {
            signature = Signature.getInstance(algorithm);
        } else {
            signature = Signature.getInstance(algorithm, provider);
        }
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }


    /**
     * 验签
     */
    public static boolean verify(byte[] message, byte[] signMessage, PublicKey publicKey, String algorithm,
                                 String provider) throws Exception {
        Signature signature;
        if (null == provider || provider.length() == 0) {
            signature = Signature.getInstance(algorithm);
        } else {
            signature = Signature.getInstance(algorithm, provider);
        }
        signature.initVerify(publicKey);
        signature.update(message);
        return signature.verify(signMessage);
    }

    public static String SSL_INSTANCE = "TLSv1.2";


    public static SSLServerSocket startServer() throws Exception{
        SSLContext ctx = SSLContext.getInstance(SSL_INSTANCE);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore tks = KeyStore.getInstance("JKS");

        ks.load(getStream("/kserver.keystore"), SERVER_KEY_STORE_PASSWORD.toCharArray());
        //信任管理器
        tks.load(getStream("/tserver.keystore"), SERVER_TRUST_KEY_STORE_PASSWORD.toCharArray());

        kmf.init(ks, SERVER_KEY_STORE_PASSWORD.toCharArray());
        tmf.init(tks);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(DEFAULT_PORT);
    }

    public static String CLIENT_KEY_STORE_PASSWORD = "123456";
    public static String CLIENT_TRUST_KEY_STORE_PASSWORD = "123456";
    public static String  DEFAULT_HOST = "127.0.0.1";

    public static InputStream getStream(String path){
        return Server.class.getResourceAsStream(path);
    }

    public static File getFile(String path){
        return new File(Server.class.getResource(path).getPath());
    }


    public static SSLSocket startClient() throws Exception {
        SSLContext ctx = SSLContext.getInstance(SSL_INSTANCE);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(getStream("/kclient.keystore"), CLIENT_KEY_STORE_PASSWORD.toCharArray());
        kmf.init(ks, CLIENT_KEY_STORE_PASSWORD.toCharArray());

        //信任管理器
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        KeyStore tks = KeyStore.getInstance("JKS");
        tks.load(getStream("/tclient.keystore"), CLIENT_TRUST_KEY_STORE_PASSWORD.toCharArray());
        tmf.init(tks);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return (SSLSocket) ctx.getSocketFactory().createSocket(DEFAULT_HOST, DEFAULT_PORT);

    }

    public static void main(String[] args) throws Exception {
        CompletableFuture.runAsync(() -> {
            try {
                @Cleanup
                SSLServerSocket sslServerSocket = startServer();
                @Cleanup
                InputStream inputStream = sslServerSocket.accept().getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    log.info("server read {}", line);
                }
            } catch (Exception e) {
                log.error("",e);
            }
        });
        TimeUnit.SECONDS.sleep(1);
        @Cleanup
        PrintWriter writer = new PrintWriter(startClient().getOutputStream());
        int i = 0;
        while (true) {
            TimeUnit.SECONDS.sleep(1);
            writer.write(("client write " + i++));
            writer.flush();

        }

    }
}

