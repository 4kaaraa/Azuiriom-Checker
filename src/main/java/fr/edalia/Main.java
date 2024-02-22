package fr.edalia;

import fr.edalia.discord.DiscordWebhook;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class Main {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Azuriom/Azuriom/releases/latest";
    private static String currentVersion;
    private static final String CONFIG_FILE_PATH = "config.json";
    private static Config config;

    private static class Config {
        private String version;
        private String webhookUrl;

        public Config(String version, String webhookUrl) {
            this.version = version;
            this.webhookUrl = webhookUrl;
        }
    }

    public static void main(String[] args) throws IOException {
        config = loadConfig();

        if (config == null) {
            Scanner input = new Scanner(System.in);

            System.out.print("[+] Version: ");
            String version = input.nextLine();

            System.out.print("[+] Webhook: ");
            String webhook = input.nextLine();

            testWebhook(webhook);

            config = new Config(version, webhook);

            saveConfig(config);
        }

        final String currentVersion = config.version;
        final String webhookUrl = config.webhookUrl;

        try {
            System.out.print("\r[+] Recuperation de l'api...");
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(GITHUB_API_URL);
            HttpResponse response = httpClient.execute(request);

            System.out.print("\r[?] Recherche d'une reponse...");
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonResponseString = EntityUtils.toString(response.getEntity());
                JSONObject jsonResponse = new JSONObject(jsonResponseString);
                String latestVersion = jsonResponse.getString("tag_name");
                String changelog = jsonResponse.getString("body");

                if (!latestVersion.equals(currentVersion)) {
                    changelog = changelog.replace("\n", "\\n");
                    changelog = changelog.replace("\r", "");
                    DiscordWebhook discordWebhook = new DiscordWebhook(webhookUrl);

                    discordWebhook.setUsername("Azuriom");

                    //discordWebhook.setContent("Nouvelle version : `" + latestVersion + "` | (" + currentVersion + ")> ");
                    discordWebhook.addEmbed(new DiscordWebhook.EmbedObject()
                            .setColor(Color.yellow)
                            .setTitle("New update: " + latestVersion)
                            .setDescription("```" + changelog + "```")
                    );

                    discordWebhook.execute();
                    System.out.print("\r[+] Nouvelle version: " + latestVersion);
                    setupNewVersion(latestVersion);
                } else {
                    System.out.print("\r[/] Pas de nouvelle version");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("[-] Erreur lors de l'analyse JSON : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[-] Une erreur s'est produite : " + e.getMessage());
        }
    }

    private static void setupNewVersion(String newVersion) {
        Config config = loadConfig();

        config.version = newVersion;

        saveConfig(config);

        System.out.println("\n[+] Version mise à jour avec succès : " + newVersion);
    }

    private static Config loadConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        Gson gson = new Gson();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                return gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void saveConfig(Config config) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void testWebhook(String webhook) throws IOException {
        DiscordWebhook testWebhook = new DiscordWebhook(webhook);

        testWebhook.setUsername("Azuriom");

        testWebhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setColor(Color.green)
                .setTitle("Configuration")
                .setDescription("> L'installation c'est correctement derouler")
                .setImage("https://tenor.com/fr/view/anime-dance-dancer-girl-anime-girl-gif-26428864")
        );

        testWebhook.execute();
    }
}