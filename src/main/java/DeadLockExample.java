
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Aspect
@Controller
@EnableAutoConfiguration
public class DeadLockExample {

    private final static Logger LOG = LoggerFactory.getLogger(DeadLockExample.class);

    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private static volatile boolean initialized = false;

    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "If you see this, the httpclient was too quick";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DeadLockExample.class, args);
        initialized = true;
        Thread thread = new Thread(()->{
            try {
                HttpResponse response = HttpClients.createDefault().execute(new HttpGet("http://localhost:8080/"));
                LOG.warn(EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                LOG.error("IO Exception: ", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        countDownLatch.await();
        throw new RuntimeException();
    }

    @Before("execution(* org.springframework.beans.factory.config.BeanPostProcessor.*(..))")
    public void webInitEvent(){
        if(initialized){
            countDownLatch.countDown();
        }
    }

}
