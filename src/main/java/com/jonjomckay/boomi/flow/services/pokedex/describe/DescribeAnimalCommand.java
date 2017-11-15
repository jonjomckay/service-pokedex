package com.jonjomckay.boomi.flow.services.pokedex.describe;

import com.jonjomckay.boomi.flow.services.pokedex.ApplicationConfiguration;
import com.jonjomckay.boomi.flow.services.pokedex.describe.DescribeAnimalAction.Input;
import com.jonjomckay.boomi.flow.services.pokedex.describe.DescribeAnimalAction.Output;
import com.manywho.sdk.api.InvokeType;
import com.manywho.sdk.api.run.elements.config.ServiceRequest;
import com.manywho.sdk.services.actions.ActionCommand;
import com.manywho.sdk.services.actions.ActionResponse;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import fastily.jwiki.core.Wiki;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;

public class DescribeAnimalCommand implements ActionCommand<ApplicationConfiguration, DescribeAnimalAction, Input, Output> {
    @Override
    public ActionResponse<Output> execute(ApplicationConfiguration configuration, ServiceRequest request, Input input) {
        if (input.getAnimal().equalsIgnoreCase("animal")) {
            return new ActionResponse<>(new Output(), InvokeType.Forward);
        }

        if (input.getAnimal().equalsIgnoreCase("mammal")) {
            return new ActionResponse<>(new Output(), InvokeType.Forward);
        }

        String sparql = "PREFIX schema: <http://schema.org/>\n" +
                "\n" +
                "SELECT ?animal ?animalLabel ?image ?article WHERE {\n" +
                "  ?animal ?label \"%s\"@en.\n" +
                "  {\n" +
                "    ?animal p:P31/ps:P31/wdt:P279* wd:Q502895.\n" +
                "  }\n" +
                "  UNION\n" +
                "  {\n" +
                "    ?animal p:P31/ps:P31/wdt:P279* wd:Q16521.\n" +
                "  }\n" +
                "  UNION\n" +
                "  {\n" +
                "    ?animal p:P31/ps:P31/wdt:P279* wd:Q729.\n" +
                "  }\n" +
                "\n" +
                "\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }\n" +
                "  OPTIONAL { ?animal wdt:P18 ?image. }\n" +
                "  OPTIONAL {\n" +
                "      ?article schema:about ?animal .\n" +
                "      ?article schema:isPartOf <https://simple.wikipedia.org/> .\n" +
                "    }\n" +
                "}";

        HttpResponse<JsonNode> response;
        try {
            response = Unirest.get("https://query.wikidata.org/sparql")
                    .queryString("format", "json")
                    .queryString("query", String.format(sparql, input.getAnimal().toLowerCase()))
                    .asJson();
        } catch (UnirestException e) {
            throw new RuntimeException("Something went wrong with the query to Wikidata: " + e.getMessage(), e);
        }

        if (response.getStatus() != 200) {
            throw new RuntimeException("Something went wrong with the query to Wikidata: " + response.getBody().toString());
        }

        JSONArray results = response.getBody().getObject().getJSONObject("results").getJSONArray("bindings");

        // If no results were found, return an empty object (as this is the most graceful way to move to the next label currently)
        if (results.length() <= 0) {
            return new ActionResponse<>(new Output(), InvokeType.Forward);
        }

        JSONObject firstResult = results.getJSONObject(0);

        String image = firstResult.getJSONObject("image").getString("value");

        String description;
        if (firstResult.isNull("article")) {
            description = "No description could be found for " + input.getAnimal();
        } else {
            String articleUrl = firstResult.getJSONObject("article").getString("value");

            String articleName = articleUrl.replace("https://simple.wikipedia.org/wiki/", "");

            String articleExtract = new Wiki("simple.wikipedia.org")
                    .getTextExtract(articleName);

            description = new Document(articleExtract)
                    .sentences()
                    .stream()
                    .limit(3)
                    .map(Sentence::toString)
                    .collect(Collectors.joining(" "));
        }

        Output output = new Output(description, image);

        return new ActionResponse<>(output, InvokeType.Forward);
    }
}
