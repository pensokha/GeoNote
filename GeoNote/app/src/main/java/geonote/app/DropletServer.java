package geonote.app;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.util.ArrayList;
import java.util.List;

import geonote.app.Model.Droplet;
import geonote.app.Model.Place;
import geonote.app.Model.PlaceDetails;
import geonote.app.Model.PlacesList;

public class DropletServer {

    private static final HttpTransport transport = new ApacheHttpTransport();
    private static final JacksonFactory jacksonFactory = new JacksonFactory();

    // The different API endpoints.
    private static final String DROPLET_GET_URL =  "http://192.168.1.153:8080/d/";
    private static final String DROPLET_POST_URL =  "http://192.168.1.153:8080/d/";

    public DropletServer()
    {

    }

    public List<Droplet> getDroplets(String userName) throws Exception {
        try {
            System.out.println("Perform droplet search ....");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);
            HttpRequest request = httpRequestFactory.buildGetRequest(new GenericUrl(DROPLET_GET_URL + userName));
            List<Droplet> droplets = new ArrayList<>();
            droplets = request.execute().parseAs(droplets.getClass());
            System.out.println(droplets);
            return droplets;

        } catch (HttpResponseException e) {
            System.err.println(e.getStatusMessage());
            throw e;
        }
    }

    public void putDroplets(String userName, List<Droplet> droplets) throws Exception {
        try {
            System.out.println("Perform droplet search ....");
            HttpRequestFactory httpRequestFactory = createRequestFactory(transport);

            HttpContent content = new JsonHttpContent(new JacksonFactory(), droplets);
            HttpRequest request = httpRequestFactory.buildPostRequest(
                    new GenericUrl(DROPLET_POST_URL + userName),
                    content);

            HttpResponse response = request.execute();
            System.out.println(response.getStatusCode());

        } catch (HttpResponseException e) {
            System.err.println(e.getStatusMessage());
            throw e;
        }
    }

    public static HttpRequestFactory createRequestFactory(final HttpTransport transport) {

        return transport.createRequestFactory(new HttpRequestInitializer() {
            public void initialize(HttpRequest request) {
                JsonObjectParser parser = new JsonObjectParser(jacksonFactory);
                request.setParser(parser);
            }
        });
    }
}
