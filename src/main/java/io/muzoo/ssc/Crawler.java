package io.muzoo.ssc;

import org.apache.commons.io.FilenameUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

public class Crawler {
    public static void main(String[] args) {
        String urlString = "https://cs.muic.mahidol.ac.th/courses/ssc/docs/";
        String outputDirectory = "F:/cs.muic.mahidol.ac.th/courses/ssc/docs/";
        Crawler crawler = new Crawler(urlString, outputDirectory);
        crawler.bfsSearch();
    }

    private final String URL, SAVE_DIR;
    private final HashSet<String> VISITED_LINK;


    public Crawler(String url, String saveDir) {
        this.URL = url;
        this.SAVE_DIR = saveDir;
        this.VISITED_LINK = new HashSet<>();

    }

    public void bfsSearch() {
        HashSet<Document> first = new HashSet<>();
        HashSet<Document> next = new HashSet<>();

        try {
            createDirectoryPath(SAVE_DIR + "index.html");
            urlDownloader(SAVE_DIR + "index.html", URL + "index.html");
            Document firstHtml = parseHtml(URL + "index.html");
            first.add(firstHtml);
            VISITED_LINK.add(SAVE_DIR + "index.html");

            while (!first.isEmpty()) {
                for (Document html : first) {
                    next.addAll(getHtml(html));
                }
                first = next;
                next = new HashSet<>();
            }

        } catch (IOException e) {
            System.err.println("Error during BFS search: " + e.getMessage());
        }
    }

    private void urlDownloader(String path, String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpUriRequestBase httpGet = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream inputStream = entity.getContent()) {
                        Path outputPath = Path.of(path);
                        Files.copy(inputStream, outputPath);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void createDirectoryPath(String path) {
        File file = new File(path);
        boolean successMaking = file.mkdirs();
        if (!successMaking) {
            System.out.println("Creat Failed");
        }
        boolean successDeleted = file.delete();
        if (!successDeleted) {
            System.out.println("Delete Failed");
        }
    }

    private String newDirectory(String path) {
        return path.replace(this.URL, this.SAVE_DIR);
    }

    private HashSet<Document> getHtml(Document html) {
        HashSet<String> newUrls = getResources(html);
        HashSet<Document> newHtml = new HashSet<>();
        for (String url : newUrls) {
            String newSaveDir = newDirectory(url);
            createDirectoryPath(newSaveDir);
            try {
                urlDownloader(newSaveDir, url);
                if (isHtml(url)) {
                    Document getHtmlPage = parseHtml(url);
                    newHtml.add(getHtmlPage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return newHtml;

    }


    private HashSet<String> getResources(Document html) {
        HashMap<String, Elements> tags = new HashMap<>();
        tags.put("link", html.select("link"));
        tags.put("a", html.select("a"));
        tags.put("img", html.select("img"));
        tags.put("frame", html.select("frame"));
        tags.put("iframe", html.select("iframe"));
        tags.put("script", html.select("script"));
        HashSet<String> url = new HashSet<>();
        for (String tag : tags.keySet()) {
            String href;
            String temp;
            String src;
            switch (tag) {
                case "link" -> {
                    for (Element i : tags.get("link")) {
                        temp = i.absUrl("href");
                        href = cleanUrl(temp);
                        if (isDownload(href)) {
                            url.add(href);
                            VISITED_LINK.add(href);
                        }
                    }
                }
                case "a" -> {
                    for (Element i : tags.get("a")) {
                        temp = i.absUrl("href");
                        href = cleanUrl(temp);
                        if (isDownload(href)) {
                            url.add(href);
                            VISITED_LINK.add(href);
                        }
                    }
                }
                case "img" -> {
                    for (Element i : tags.get("img")) {
                        temp = i.absUrl("src");
                        src = cleanUrl(temp);
                        if (isDownload(src)) {
                            url.add(src);
                            VISITED_LINK.add(src);
                        }
                    }
                }
                case "frame" -> {
                    for (Element i : tags.get("frame")) {
                        temp = i.absUrl("src");
                        src = cleanUrl(temp);
                        if (isDownload(src)) {
                            url.add(src);
                            VISITED_LINK.add(src);
                        }
                    }
                }
                case "iframe" -> {
                    for (Element i : tags.get("iframe")) {
                        temp = i.absUrl("src");
                        src = cleanUrl(temp);
                        if (isDownload(src)) {
                            url.add(src);
                            VISITED_LINK.add(src);
                        }
                    }
                }
                case "script" -> {
                    for (Element i : tags.get("script")) {
                        temp = i.absUrl("src");
                        src = cleanUrl(temp);
                        if (isDownload(src)) {
                            url.add(src);
                            VISITED_LINK.add(src);
                        }
                    }
                }
            }
        }
        return url;
    }

    private String cleanUrl(String url) {
        if (url.contains("#")) {
            return url.substring(0, url.indexOf("#"));
        }
        return url;
    }

    private boolean isDownload(String url) {
        return !VISITED_LINK.contains(url) && url.contains(this.URL)
                && url.contains("." + FilenameUtils.getExtension(url))
                && !url.contains("?") && !url.contains("'") && !url.contains("%");

    }

    private boolean isHtml(String url) {
        return url.contains(".html");
    }

    private Document parseHtml(String url) throws IOException {
        return Jsoup.connect(url).get();
    }
}