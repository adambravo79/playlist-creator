package com.example.playlist_creator.helper;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class YouTubeAuthenticator {
    private static final String CLIENT_SECRETS = "src/main/resources/credentials.json"; // Caminho para o arquivo JSON com credenciais // Caminho para o arquivo JSON com credenciais
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens"; // Diretório para armazenar tokens
    private static final String APPLICATION_NAME = "Criador de Playlist"; // Nome da sua aplicação
    private static GoogleAuthorizationCodeFlow flow;

    static {
        try {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(CLIENT_SECRETS));
    
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
                    Collections.singleton("https://www.googleapis.com/auth/youtube.force-ssl"))
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            flow = null; // Certifique-se de que flow é nulo se a inicialização falhar
        }
    }

    // Método para autenticar o YouTube
    public static YouTube authenticateYouTube(HttpServletRequest request, HttpServletResponse response) throws GeneralSecurityException, IOException {
        // Carregar credenciais
        Credential credential = flow.loadCredential("user");

        if (credential == null || credential.getExpiresInSeconds() <= 60) {
            // Se não houver credenciais ou se estiverem prestes a expirar, inicie o fluxo de autorização
            String redirectUri = flow.newAuthorizationUrl().setRedirectUri("http://localhost:8080/playlist/oauth2callback").build();
            response.sendRedirect(redirectUri);
            return null; // Retorne null enquanto aguarda o redirecionamento
        }


        // Construir e retornar o serviço YouTube
        return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // Método para lidar com o callback do OAuth
    public static void handleOAuthCallback(String code) throws GeneralSecurityException, IOException {
        // Trocar o código de autorização pelo token de acesso
        GoogleTokenResponse credential = flow.newTokenRequest(code).setRedirectUri("http://localhost:8080/playlist/oauth2callback").execute();
        flow.createAndStoreCredential(credential, "user");
    }
}
