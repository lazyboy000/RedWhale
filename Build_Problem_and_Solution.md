# Troubleshooting the "PKIX path building failed" Gradle Error

## 1. The Problem

When trying to build the RedWhale project, the build fails with a `PKIX path building failed` error. This error occurs when Gradle attempts to download project dependencies from online repositories over a secure HTTPS connection.

The full error message looks something like this:

```
sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

## 2. Explanation

This is a common Java SSL/TLS issue. Here's a breakdown of what's happening:

*   **Secure Connections:** Gradle downloads project dependencies from repositories like Maven Central and Google's Maven repository. For security, these connections are made over HTTPS.
*   **Certificate Validation:** When your computer connects to these repositories, they present an SSL certificate to prove their identity. Your Java environment then checks if this certificate is trusted.
*   **The Truststore:** The Java virtual machine (JVM) that Gradle uses has a "truststore" which contains a list of trusted Certificate Authorities (CAs). If the certificate from the server is signed by a CA in the truststore, the connection is considered secure.
*   **The Error:** The `PKIX path building failed` error means that the Java environment could not validate the certificate presented by the server. This is very common in corporate environments where a proxy server or firewall intercepts all network traffic (including HTTPS) for security scanning. The proxy then re-encrypts the traffic with its own certificate, which is not in Java's default truststore.

## 3. Solutions

Here are a few ways to solve this issue, from the most secure to the least secure.

### Solution 1: Add the Certificate to the Truststore (Recommended)

This is the most secure and correct way to solve this problem, especially in a corporate environment.

1.  **Obtain the Certificate:** You need to get the root certificate of your corporate proxy/firewall. This is usually provided by your IT department. They might provide it as a `.cer` or `.pem` file.

2.  **Locate the Truststore:** Find the truststore of the Java installation that Gradle is using.
    *   Run `gradlew --version` in your project directory to see the "JVM" path. This is your `JAVA_HOME`.
    *   The truststore is typically located at `JAVA_HOME\lib\security\cacerts`.

3.  **Import the Certificate:** Use the `keytool` utility (which comes with the JDK) to import the certificate into the truststore. Open a command prompt or terminal with administrator privileges and run the following command:

    ```bash
    keytool -import -trustcacerts -keystore "C:\Path\To\Your\Java\lib\security\cacerts" -storepass changeit -noprompt -alias "MyCorpCert" -file "C:\Path\To\Your\certificate.cer"
    ```

    *   **`-keystore`**: Replace with the full path to your `cacerts` file.
    *   **`-storepass`**: The default password for the `cacerts` truststore is `changeit`.
    *   **`-alias`**:  Give the certificate a unique name (e.g., "MyCorpCert").
    *   **`-file`**: Replace with the full path to the certificate file you obtained from your IT department.

### Solution 2: Run in a Different Network

The simplest solution, if possible, is to build the project on a different network that does not have this SSL interception issue. A home network or a personal hotspot will usually work.

### Solution 3: Disable SSL Verification (Not Recommended)

This solution is **not recommended** for production environments as it exposes your build to security risks. However, it can be a quick way to get your project to build.

Add the following lines to your `gradle.properties` file in the root of the RedWhale project:

```
systemProp.javax.net.ssl.trustStore=NONE
```

This tells the JVM to not use any truststore, effectively disabling SSL certificate validation.

After trying any of these solutions, you should be able to build the project by running `.\gradlew.bat build` from the project's root directory.
