package com.streamapp.streamappbackend.service.playlist;

import com.streamapp.streamappbackend.dto.MediaItemDto;
import com.streamapp.streamappbackend.dto.PlaylistDto;
import com.streamapp.streamappbackend.dto.PlaylistSummaryDto;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.Playlist;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.ConflictException;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.repository.PlaylistRepository;
import com.streamapp.streamappbackend.service.explorer.PathAccessValidator;
import com.streamapp.streamappbackend.service.media.MediaItemService;
import com.streamapp.streamappbackend.service.streaming.StreamTicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final MediaItemService mediaItemService;
    private final PathAccessValidator pathAccessValidator;
    private final StreamTicketService streamTicketService;

    public PlaylistService(PlaylistRepository playlistRepository,
                           MediaItemService mediaItemService,
                           PathAccessValidator pathAccessValidator,
                           StreamTicketService streamTicketService) {
        this.playlistRepository = playlistRepository;
        this.mediaItemService = mediaItemService;
        this.pathAccessValidator = pathAccessValidator;
        this.streamTicketService = streamTicketService;
    }

    @Transactional
    public PlaylistDto create(User user, String name) {
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setOwner(user);
        Playlist saved = playlistRepository.save(playlist);
        return toDto(user, saved);
    }

    @Transactional(readOnly = true)
    public List<PlaylistSummaryDto> list(User user) {
        return playlistRepository.findAllByOwner(user).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistDto get(User user, Long id) {
        Playlist playlist = owned(user, id);
        return toDto(user, playlist);
    }

    @Transactional
    public PlaylistDto rename(User user, Long id, String name) {
        Playlist playlist = owned(user, id);
        playlist.setName(name);
        return toDto(user, playlistRepository.save(playlist));
    }

    @Transactional
    public void delete(User user, Long id) {
        Playlist playlist = owned(user, id);
        playlistRepository.delete(playlist);
    }

    @Transactional
    public PlaylistDto addItem(User user, Long id, String rawPath) {
        Playlist playlist = owned(user, id);
        Path resolved = pathAccessValidator.resolveWithinRoots(user, rawPath);
        MediaItem item = mediaItemService.findOrCreate(resolved);

        boolean exists = playlist.getItems().stream()
                .anyMatch(i -> i.getPath().equals(item.getPath()));
        if (exists) {
            throw new ConflictException("El elemento ya está en la playlist");
        }
        playlist.getItems().add(item);
        return toDto(user, playlistRepository.save(playlist));
    }

    @Transactional
    public PlaylistDto removeItem(User user, Long id, String rawPath) {
        Playlist playlist = owned(user, id);
        Path resolved = pathAccessValidator.resolveWithinRoots(user, rawPath);
        String targetPath = resolved.toString();

        boolean removed = playlist.getItems().removeIf(i -> i.getPath().equals(targetPath));
        if (!removed) {
            throw new NotFoundException("El elemento no está en la playlist");
        }
        return toDto(user, playlistRepository.save(playlist));
    }

    private Playlist owned(User user, Long id) {
        return playlistRepository.findByIdAndOwner(id, user)
                .orElseThrow(() -> new NotFoundException("Playlist no encontrada"));
    }

    private PlaylistSummaryDto toSummary(Playlist playlist) {
        return new PlaylistSummaryDto(
                playlist.getId(),
                playlist.getName(),
                playlist.getCreatedAt(),
                playlist.getItems().size());
    }

    private PlaylistDto toDto(User user, Playlist playlist) {
        List<MediaItemDto> items = playlist.getItems().stream()
                .map(item -> {
                    String streamUrl = "/api/stream/"
                            + streamTicketService.generate(user.getUsername(), item.getPath());
                    return new MediaItemDto(
                            item.getId(),
                            item.getTitle(),
                            item.getPath(),
                            item.getMediaType(),
                            item.getContentType(),
                            item.getSizeBytes(),
                            item.getLastModified(),
                            streamUrl,
                            item.getPosterUrl(),
                            item.getMediaTypeDetail());
                })
                .toList();
        return new PlaylistDto(
                playlist.getId(),
                playlist.getName(),
                playlist.getCreatedAt(),
                items);
    }
}