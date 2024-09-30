package com.example.playlist_creator.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import com.example.playlist_creator.helper.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.google.api.services.youtube.model.PlaylistItemSnippet;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/playlist")
public class PlaylistCreatorController {

    // Método para extrair o ID do vídeo
    private String extractVideoId(String url) {
        String pattern = "(?:v=|\\/)([a-zA-Z0-9_-]{11})";
        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Método para criar uma nova playlist
    private String createPlaylist(YouTube youtube, String title, String description, String privacy)
            throws IOException {
        PlaylistSnippet snippet = new PlaylistSnippet();
        snippet.setTitle(title);
        snippet.setDescription(description);

        PlaylistStatus status = new PlaylistStatus();
        status.setPrivacyStatus(privacy);

        Playlist playlist = new Playlist();
        playlist.setSnippet(snippet);
        playlist.setStatus(status);

        Playlist createdPlaylist = youtube.playlists().insert("snippet,status", playlist).execute();
        return createdPlaylist.getId();
    }

    // Método para adicionar um vídeo à playlist
    private void addVideoToPlaylist(YouTube youtube, String videoId, String playlistId) throws IOException {
        // Criar um objeto ResourceId para especificar o tipo de recurso e o ID do vídeo
        ResourceId resourceId = new ResourceId();
        resourceId.setKind("youtube#video");
        resourceId.setVideoId(videoId);

        // Criar o snippet do PlaylistItem para associar o vídeo à playlist
        PlaylistItemSnippet snippet = new PlaylistItemSnippet();
        snippet.setPlaylistId(playlistId);
        snippet.setResourceId(resourceId);

        // Criar o PlaylistItem
        PlaylistItem playlistItem = new PlaylistItem();
        playlistItem.setSnippet(snippet);

        // Inserir o item na playlist
        youtube.playlistItems().insert("snippet", playlistItem).execute();
    }

    @GetMapping("/auth")
    public void authenticate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, GeneralSecurityException {
        YouTubeAuthenticator.authenticateYouTube(request, response);
    }

    @GetMapping("/oauth2callback")
    public String handleOAuthCallback(@RequestParam("code") String code, Model model, HttpServletRequest request,
            HttpServletResponse response) {
        try {
            YouTubeAuthenticator.handleOAuthCallback(code);

            // Após autenticar, você pode redirecionar para a página que permite criar a
            // playlist
            return "redirect:/"; // Exemplo de redirecionamento para o formulário de criação
        } catch (GeneralSecurityException | IOException e) {
            model.addAttribute("mensagem", "Erro ao processar o callback: " + e.getMessage());
            return "error"; // Redireciona para a página de erro
        }
    }

    @PostMapping("/criar")
    public ResponseEntity<String> criarPlaylist(@RequestParam("nome_playlist") String nomePlaylist,
            @RequestParam("url_file") MultipartFile urlFile,
            @RequestParam("visibilidade") String visibilidade,
            Model model, HttpServletRequest request, HttpServletResponse response) {

        List<String> videoIds = new ArrayList<>();
        YouTube youtubeService;

        // Autentica o YouTube
        try {
            youtubeService = YouTubeAuthenticator.authenticateYouTube(request, response);
            if (youtubeService == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erro de autenticação.");
            }
        } catch (GeneralSecurityException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao autenticar: " + e.getMessage());
        }

        // Ler URLs do arquivo .txt
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlFile.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String videoId = extractVideoId(line.trim());
                if (videoId != null) {
                    videoIds.add(videoId);
                }
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao ler o arquivo: " + e.getMessage());
        }

        // Criar a playlist e adicionar vídeos
        try {
            String playlistId = createPlaylist(youtubeService, nomePlaylist, "Descrição da playlist", visibilidade);
            for (String videoId : videoIds) {
                addVideoToPlaylist(youtubeService, videoId, playlistId);
            }

            // Construir o link da playlist
            String playlistLink = "https://www.youtube.com/playlist?list=" + playlistId;
            return ResponseEntity
                    .ok("Playlist criada com sucesso: " + nomePlaylist + ". Acesse sua playlist aqui: " + playlistLink);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criar a playlist: " + e.getMessage());
        }
    }
}

/*
 * 
 * import org.springframework.web.bind.annotation.*;
 * import org.springframework.web.multipart.MultipartFile;
 * import org.springframework.ui.Model;
 * import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
 * import com.google.api.client.json.JsonFactory;
 * import com.google.api.client.json.gson.GsonFactory;
 * import com.google.api.client.json.jackson2.JacksonFactory;
 * import com.google.api.services.youtube.YouTube;
 * import com.google.api.services.youtube.model.Playlist;
 * import com.google.api.services.youtube.model.PlaylistItem;
 * import com.google.api.services.youtube.model.PlaylistSnippet;
 * import com.google.api.services.youtube.model.PlaylistStatus;
 * 
 * import java.io.BufferedReader;
 * import java.io.InputStreamReader;
 * import java.io.IOException;
 * import java.security.GeneralSecurityException;
 * import java.util.ArrayList;
 * import java.util.List;
 * 
 * @RestController
 * 
 * @RequestMapping("/playlist")
 * public class PlaylistCreatorController {
 * 
 * private static final JsonFactory JSON_FACTORY =
 * GsonFactory.getDefaultInstance();
 * private static YouTube youtubeService;
 * 
 * // Método para autenticar o YouTube
 * private YouTube authenticateYouTube() throws GeneralSecurityException,
 * IOException {
 * // Autenticação semelhante ao Python
 * // Aqui, configure sua autenticação com a API do YouTube.
 * // Você pode usar um fluxo de autenticação semelhante ao seu código Python.
 * return youtubeService;
 * }
 * 
 * // Método para extrair o ID do vídeo
 * private String extractVideoId(String url) {
 * String pattern = "(?:v=|\\/)([a-zA-Z0-9_-]{11})";
 * java.util.regex.Pattern compiledPattern =
 * java.util.regex.Pattern.compile(pattern);
 * java.util.regex.Matcher matcher = compiledPattern.matcher(url);
 * return matcher.find() ? matcher.group(1) : null;
 * }
 * 
 * // Método para criar uma nova playlist
 * private String createPlaylist(YouTube youtube, String title, String
 * description, String privacy) throws IOException {
 * PlaylistSnippet snippet = new PlaylistSnippet();
 * snippet.setTitle(title);
 * snippet.setDescription(description);
 * 
 * PlaylistStatus status = new PlaylistStatus();
 * status.setPrivacyStatus(privacy);
 * 
 * Playlist playlist = new Playlist();
 * playlist.setSnippet(snippet);
 * playlist.setStatus(status);
 * 
 * Playlist createdPlaylist = youtube.playlists().insert("snippet,status",
 * playlist).execute();
 * return createdPlaylist.getId();
 * }
 * 
 * // Método para adicionar um vídeo à playlist
 * private void addVideoToPlaylist(YouTube youtube, String videoId, String
 * playlistId) throws IOException {
 * PlaylistItem playlistItem = new PlaylistItem();
 * playlistItem.setSnippet(new
 * PlaylistItem.Snippet().setPlaylistId(playlistId).setResourceId(new
 * ResourceId().setKind("youtube#video").setVideoId(videoId)));
 * youtube.playlistItems().insert("snippet", playlistItem).execute();
 * }
 * 
 * @PostMapping("/criar")
 * public String criarPlaylist(@RequestParam("nome_playlist") String
 * nomePlaylist,
 * 
 * @RequestParam("url_file") MultipartFile urlFile,
 * 
 * @RequestParam("visibilidade") String visibilidade,
 * Model model) {
 * List<String> videoIds = new ArrayList<>();
 * 
 * try (BufferedReader reader = new BufferedReader(new
 * InputStreamReader(urlFile.getInputStream()))) {
 * String line;
 * while ((line = reader.readLine()) != null) {
 * String videoId = extractVideoId(line.trim());
 * if (videoId != null) {
 * videoIds.add(videoId);
 * }
 * }
 * } catch (IOException e) {
 * model.addAttribute("mensagem", "Erro ao ler o arquivo: " + e.getMessage());
 * return "index"; // Redireciona para a página index
 * }
 * 
 * try {
 * youtubeService = authenticateYouTube(); // Autenticar
 * String playlistId = createPlaylist(youtubeService, nomePlaylist,
 * "Descrição da playlist", visibilidade); // Criar a playlist
 * 
 * // Adicionar vídeos à nova playlist
 * for (String videoId : videoIds) {
 * addVideoToPlaylist(youtubeService, videoId, playlistId);
 * }
 * model.addAttribute("mensagem", "Playlist criada com sucesso: " +
 * nomePlaylist);
 * } catch (GeneralSecurityException | IOException e) {
 * model.addAttribute("mensagem", "Erro ao criar a playlist: " +
 * e.getMessage());
 * }
 * 
 * return "index"; // Redireciona para a página index
 * }
 * }/*
 * 
 * 
 * import org.springframework.web.bind.annotation.*;
 * import org.springframework.web.multipart.MultipartFile;
 * import org.springframework.ui.Model;
 * import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
 * import com.google.api.client.json.JsonFactory;
 * import com.google.api.client.json.gson.GsonFactory;
 * import com.google.api.client.json.jackson2.JacksonFactory;
 * import com.google.api.services.youtube.YouTube;
 * import com.google.api.services.youtube.model.Playlist;
 * import com.google.api.services.youtube.model.PlaylistItem;
 * import com.google.api.services.youtube.model.PlaylistSnippet;
 * import com.google.api.services.youtube.model.PlaylistStatus;
 * import com.google.api.services.youtube.model.ResourceId;
 * 
 * import java.io.BufferedReader;
 * import java.io.InputStreamReader;
 * import java.io.IOException;
 * import java.security.GeneralSecurityException;
 * import java.util.ArrayList;
 * import java.util.List;
 * 
 * @RestController
 * 
 * @RequestMapping("/playlist")
 * public class PlaylistCreatorController {
 * 
 * 
 * 
 * 
 * private static final JsonFactory JSON_FACTORY =
 * GsonFactory.getDefaultInstance();;
 * private static YouTube youtubeService;
 * 
 * // Método para autenticar o YouTube
 * private YouTube authenticateYouTube() throws GeneralSecurityException,
 * IOException {
 * // Autenticação semelhante ao Python
 * // Aqui, configure sua autenticação com a API do YouTube.
 * // Você pode usar um fluxo de autenticação semelhante ao seu código Python.
 * return youtubeService;
 * }
 * 
 * // Método para extrair o ID do vídeo
 * private String extractVideoId(String url) {
 * String pattern = "(?:v=|\\/)([a-zA-Z0-9_-]{11})";
 * java.util.regex.Pattern compiledPattern =
 * java.util.regex.Pattern.compile(pattern);
 * java.util.regex.Matcher matcher = compiledPattern.matcher(url);
 * return matcher.find() ? matcher.group(1) : null;
 * }
 * 
 * // Método para criar uma nova playlist
 * private String createPlaylist(YouTube youtube, String title, String
 * description, String privacy) throws IOException {
 * PlaylistSnippet snippet = new PlaylistSnippet();
 * snippet.setTitle(title);
 * snippet.setDescription(description);
 * 
 * PlaylistStatus status = new PlaylistStatus();
 * status.setPrivacyStatus(privacy);
 * 
 * Playlist playlist = new Playlist();
 * playlist.setSnippet(snippet);
 * playlist.setStatus(status);
 * 
 * Playlist createdPlaylist = youtube.playlists().insert("snippet,status",
 * playlist).execute();
 * return createdPlaylist.getId();
 * }
 * 
 * // Método para adicionar um vídeo à playlist
 * private void addVideoToPlaylist(YouTube youtube, String videoId, String
 * playlistId) throws IOException {
 * PlaylistItem playlistItem = new PlaylistItem();
 * playlistItem.setSnippet(new
 * PlaylistItem.Snippet().setPlaylistId(playlistId).setResourceId(new
 * ResourceId().setKind("youtube#video").setVideoId(videoId)));
 * youtube.playlistItems().insert("snippet", playlistItem).execute();
 * }
 * 
 * @PostMapping("/criar")
 * public String criarPlaylist(@RequestParam("nome_playlist") String
 * nomePlaylist,
 * 
 * @RequestParam("url_file") MultipartFile urlFile,
 * 
 * @RequestParam("visibilidade") String visibilidade,
 * Model model) {
 * List<String> videoIds = new ArrayList<>();
 * 
 * try (BufferedReader reader = new BufferedReader(new
 * InputStreamReader(urlFile.getInputStream()))) {
 * String line;
 * while ((line = reader.readLine()) != null) {
 * String videoId = extractVideoId(line.trim());
 * if (videoId != null) {
 * videoIds.add(videoId);
 * }
 * }
 * } catch (IOException e) {
 * model.addAttribute("mensagem", "Erro ao ler o arquivo: " + e.getMessage());
 * return "index"; // Redireciona para a página index
 * }
 * 
 * try {
 * youtubeService = authenticateYouTube(); // Autenticar
 * String playlistId = createPlaylist(youtubeService, nomePlaylist,
 * "Descrição da playlist", visibilidade); // Criar a playlist
 * 
 * // Adicionar vídeos à nova playlist
 * for (String videoId : videoIds) {
 * addVideoToPlaylist(youtubeService, videoId, playlistId);
 * }
 * model.addAttribute("mensagem", "Playlist criada com sucesso: " +
 * nomePlaylist);
 * } catch (GeneralSecurityException | IOException e) {
 * model.addAttribute("mensagem", "Erro ao criar a playlist: " +
 * e.getMessage());
 * }
 * 
 * return "index"; // Redireciona para a página index
 * }
 * }
 * 
 */