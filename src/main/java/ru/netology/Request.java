package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final String path;
    private final String rawPath;
    private final String protocol;
    private final Map<String, String> queryParams;

    public Request(String requestLine) throws IllegalArgumentException {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid request line: " + requestLine);
        }

        this.method = parts[0];
        this.rawPath = parts[1];
        this.protocol = parts[2];

        // Парсим путь и query параметры
        try {
            URI uri = new URI(this.rawPath);
            this.path = uri.getPath();

            // Используем URLEncodedUtils для парсинга query параметров
            List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
            this.queryParams = params.stream()
                    .collect(Collectors.toMap(
                            NameValuePair::getName,
                            NameValuePair::getValue,
                            (v1, v2) -> v2 // при дубликатах берем последнее значение
                    ));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + this.rawPath, e);
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

     public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }


    public boolean hasQueryParam(String name) {
        return queryParams.containsKey(name);
    }

    @Override
    public String toString() {
        return String.format("Request{method='%s', path='%s', queryParams=%s}",
                method, path, queryParams);
    }
}
