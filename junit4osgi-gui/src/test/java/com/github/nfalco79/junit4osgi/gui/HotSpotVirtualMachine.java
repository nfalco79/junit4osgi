/*
 * Decompiled with CFR 0_123.
 */
package sun.tools.attach;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class HotSpotVirtualMachine
extends VirtualMachine {
    private static final int JNI_ENOMEM = -4;
    private static final int ATTACH_ERROR_BADJAR = 100;
    private static final int ATTACH_ERROR_NOTONCP = 101;
    private static final int ATTACH_ERROR_STARTFAIL = 102;
    private static final String MANAGMENT_PREFIX = "com.sun.management.";
    private static long defaultAttachTimeout = 5000;
    private volatile long attachTimeout;

    HotSpotVirtualMachine(AttachProvider attachProvider, String string) {
        super(attachProvider, string);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void loadAgentLibrary(String string, boolean bl, String string2) throws AgentLoadException, AgentInitializationException, IOException {
        Object[] arrobject = new Object[3];
        arrobject[0] = string;
        arrobject[1] = bl ? "true" : "false";
        arrobject[2] = string2;
        InputStream inputStream = this.execute("load", arrobject);
        try {
            int n = this.readInt(inputStream);
            if (n != 0) {
                throw new AgentInitializationException("Agent_OnAttach failed", n);
            }
        }
        finally {
            inputStream.close();
        }
    }

    @Override
    public void loadAgentLibrary(String string, String string2) throws AgentLoadException, AgentInitializationException, IOException {
        this.loadAgentLibrary(string, false, string2);
    }

    @Override
    public void loadAgentPath(String string, String string2) throws AgentLoadException, AgentInitializationException, IOException {
        this.loadAgentLibrary(string, true, string2);
    }

    @Override
    public void loadAgent(String string, String string2) throws AgentLoadException, AgentInitializationException, IOException {
        String string3 = string;
        if (string2 != null) {
            string3 = string3 + "=" + string2;
        }
        try {
            this.loadAgentLibrary("instrument", string3);
        }
        catch (AgentLoadException agentLoadException) {
            throw new InternalError("instrument library is missing in target VM", agentLoadException);
        }
        catch (AgentInitializationException agentInitializationException) {
            int n = agentInitializationException.returnValue();
            switch (n) {
                case -4: {
                    throw new AgentLoadException("Insuffient memory");
                }
                case 100: {
                    throw new AgentLoadException("Agent JAR not found or no Agent-Class attribute");
                }
                case 101: {
                    throw new AgentLoadException("Unable to add JAR file to system class path");
                }
                case 102: {
                    throw new AgentInitializationException("Agent JAR loaded but agent failed to initialize");
                }
            }
            throw new AgentLoadException("Failed to load agent - unknown reason: " + n);
        }
    }

    @Override
    public Properties getSystemProperties() throws IOException {
        Properties properties;
        InputStream inputStream = null;
        properties = new Properties();
        try {
            inputStream = this.executeCommand("properties", new Object[0]);
            properties.load(inputStream);
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return properties;
    }

    @Override
    public Properties getAgentProperties() throws IOException {
        Properties properties;
        InputStream inputStream = null;
        properties = new Properties();
        try {
            inputStream = this.executeCommand("agentProperties", new Object[0]);
            properties.load(inputStream);
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return properties;
    }

    private static boolean checkedKeyName(Object object) {
        if (!(object instanceof String)) {
            throw new IllegalArgumentException("Invalid option (not a String): " + object);
        }
        if (!((String)object).startsWith("com.sun.management.")) {
            throw new IllegalArgumentException("Invalid option: " + object);
        }
        return true;
    }

    private static String stripKeyName(Object object) {
        return ((String)object).substring("com.sun.management.".length());
    }

    @Override
    public void startManagementAgent(Properties properties) throws IOException {
        if (properties == null) {
            throw new NullPointerException("agentProperties cannot be null");
        }
        String string = properties.entrySet().stream().filter(entry -> HotSpotVirtualMachine.checkedKeyName(entry.getKey())).map(entry -> HotSpotVirtualMachine.stripKeyName(entry.getKey()) + "=" + this.escape(entry.getValue())).collect(Collectors.joining(" "));
        this.executeJCmd("ManagementAgent.start " + string);
    }

    private String escape(Object object) {
        String string = object.toString();
        if (string.contains(" ")) {
            return "'" + string + "'";
        }
        return string;
    }

    @Override
    public String startLocalManagementAgent() throws IOException {
        this.executeJCmd("ManagementAgent.start_local");
        return this.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
    }

    public void localDataDump() throws IOException {
        this.executeCommand("datadump", new Object[0]).close();
    }

    public /* varargs */ InputStream remoteDataDump(Object ... arrobject) throws IOException {
        return this.executeCommand("threaddump", arrobject);
    }

    public /* varargs */ InputStream dumpHeap(Object ... arrobject) throws IOException {
        return this.executeCommand("dumpheap", arrobject);
    }

    public /* varargs */ InputStream heapHisto(Object ... arrobject) throws IOException {
        return this.executeCommand("inspectheap", arrobject);
    }

    public InputStream setFlag(String string, String string2) throws IOException {
        return this.executeCommand("setflag", string, string2);
    }

    public InputStream printFlag(String string) throws IOException {
        return this.executeCommand("printflag", string);
    }

    public InputStream executeJCmd(String string) throws IOException {
        return this.executeCommand("jcmd", string);
    }

    /* varargs */ abstract InputStream execute(String var1, Object ... var2) throws AgentLoadException, IOException;

    private /* varargs */ InputStream executeCommand(String string, Object ... arrobject) throws IOException {
        try {
            return this.execute(string, arrobject);
        }
        catch (AgentLoadException agentLoadException) {
            throw new InternalError("Should not get here", agentLoadException);
        }
    }

    int readInt(InputStream inputStream) throws IOException {
        int n;
        int n2;
        StringBuilder stringBuilder = new StringBuilder();
        byte[] arrby = new byte[1];
        do {
            if ((n2 = inputStream.read(arrby, 0, 1)) <= 0) continue;
            n = arrby[0];
            if (n == 10) break;
            stringBuilder.append((char)n);
        } while (n2 > 0);
        if (stringBuilder.length() == 0) {
            throw new IOException("Premature EOF");
        }
        try {
            n = Integer.parseInt(stringBuilder.toString());
        }
        catch (NumberFormatException numberFormatException) {
            throw new IOException("Non-numeric value found - int expected");
        }
        return n;
    }

    String readErrorMessage(InputStream inputStream) throws IOException {
        int n;
        byte[] arrby = new byte[1024];
        StringBuffer stringBuffer = new StringBuffer();
        while ((n = inputStream.read(arrby)) != -1) {
            stringBuffer.append(new String(arrby, 0, n, "UTF-8"));
        }
        return stringBuffer.toString();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    long attachTimeout() {
        if (this.attachTimeout == 0) {
            HotSpotVirtualMachine hotSpotVirtualMachine = this;
            synchronized (hotSpotVirtualMachine) {
                if (this.attachTimeout == 0) {
                    try {
                        String string = System.getProperty("sun.tools.attach.attachTimeout");
                        this.attachTimeout = Long.parseLong(string);
                    }
                    catch (SecurityException securityException) {
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                    if (this.attachTimeout <= 0) {
                        this.attachTimeout = defaultAttachTimeout;
                    }
                }
            }
        }
        return this.attachTimeout;
    }
}

