import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class JMeterZMQSampler extends AbstractJavaSamplerClient {
    
    private ZContext context;
    private ZMQ.Socket requester;
    
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("host", "localhost");
        defaultParameters.addArgument("port", "5556");
        defaultParameters.addArgument("operation", "INFO");
        defaultParameters.addArgument("book", "978-0134685991");
        return defaultParameters;
    }
    
    @Override
    public void setupTest(JavaSamplerContext context) {
        this.context = new ZContext();
        this.requester = this.context.createSocket(ZMQ.REQ);
        String host = context.getParameter("host");
        String port = context.getParameter("port");
        this.requester.connect("tcp://" + host + ":" + port);
    }
    
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        
        try {
            String operation = context.getParameter("operation");
            String book = context.getParameter("book");
            String user = "jmeter_" + Thread.currentThread().getId();
            
            String request = operation + "|" + book + "|" + user;
            
            requester.send(request);
            String response = requester.recvStr();
            
            result.sampleEnd();
            result.setResponseData(response, "UTF-8");
            result.setResponseMessage("OK");
            result.setSuccessful(response != null && response.startsWith("OK"));
            result.setResponseCodeOK();
            
        } catch (Exception e) {
            result.sampleEnd();
            result.setSuccessful(false);
            result.setResponseMessage("Error: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public void teardownTest(JavaSamplerContext context) {
        if (requester != null) {
            requester.close();
        }
        if (this.context != null) {
            this.context.close();
        }
    }
}
