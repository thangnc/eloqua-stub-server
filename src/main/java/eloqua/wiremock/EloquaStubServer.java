package eloqua.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcherExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * @author thangnc
 */
public class EloquaStubServer
{
    private static final Logger LOG = LoggerFactory.getLogger(EloquaStubServer.class);
    private static final int MAX_ALLOW_REQUEST_SIZE = 23_000_000;
    private static final int MAX_ALLOW_STAGING_SIZE = 250_000_000;
    private final WireMockServer wm;
    private static int stagingSize = 0;

    public EloquaStubServer()
    {
        wm = new WireMockServer(WireMockConfiguration.options()
                                                     .port(Integer.parseInt(System.getenv("PORT")))
                                                     .usingFilesUnderClasspath("wiremock"));
        wm.stubFor(WireMock.requestMatching(bodyTooLarge())
                           .willReturn(WireMock.aResponse()
                                               .withStatus(422)
                                               .withBody("Entity is too large")));
        wm.stubFor(WireMock.requestMatching(uploadData())
                           .willReturn(WireMock.aResponse()
                                               .withStatus(422)
                                               .withBody("Staging server is too large")));
    }

    public void start()
    {
        wm.start();
    }

    public void stop()
    {
        wm.stop();
    }

    private RequestMatcherExtension bodyTooLarge()
    {
        return new RequestMatcherExtension()
        {
            @Override
            public MatchResult match(Request request, Parameters parameters)
            {
                return MatchResult.of(request.getBody().length > MAX_ALLOW_REQUEST_SIZE);
            }
        };
    }

    private RequestMatcherExtension uploadData()
    {
        return new RequestMatcherExtension()
        {
            @Override
            public MatchResult match(Request request, Parameters parameters)
            {
                if (request.getUrl()
                           .equals("/api/bulk/2.0/contacts/imports/1182/data")) {
                    stagingSize += request.getBody().length;
                }

                return MatchResult.of(stagingSize > MAX_ALLOW_STAGING_SIZE);
            }
        };
    }

    public static void main(final String[] args)
    {
        try {
            EloquaStubServer server = new EloquaStubServer();
            server.start();
            LOG.info("\n----------------------------------------------------------\n\t"
                             + "Stub server is running! Access URLs:\n\t"
                             + "Local: \t\thttp://localhost:{}\n\t"
                             + "External: \thttp://{}:{}\n\t"
                             + "\n----------------------------------------------------------",
                     Integer.parseInt(System.getenv("PORT")),
                     InetAddress.getLocalHost()
                                .getHostAddress(),
                     Integer.parseInt(System.getenv("PORT")));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
