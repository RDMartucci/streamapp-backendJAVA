package com.streamapp.streamappbackend.service.favorite;

import com.streamapp.streamappbackend.dto.FavoriteDto;
import com.streamapp.streamappbackend.dto.MediaItemDto;
import com.streamapp.streamappbackend.entity.Favorite;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.ConflictException;
import com.streamapp.streamappbackend.repository.FavoriteRepository;
import com.streamapp.streamappbackend.service.explorer.PathAccessValidator;
import com.streamapp.streamappbackend.service.media.MediaItemService;
import com.streamapp.streamappbackend.service.streaming.StreamTicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MediaItemService mediaItemService;
    private final PathAccessValidator pathAccessValidator;
    private final StreamTicketService streamTicketService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           MediaItemService mediaItemService,
                           PathAccessValidator pathAccessValidator,
                           StreamTicketService streamTicketService) {
        this.favoriteRepository = favoriteRepository;
        this.mediaItemService = mediaItemService;
        this.pathAccessValidator = pathAccessValidator;
        this.streamTicketService = streamTicketService;
    }

    @Transactional(readOnly = true)
    public List<FavoriteDto> list(User user) {
        return favoriteRepository.findAllByUser(user).stream()
                .map(f -> toDto(user, f.getMediaItem(), f.getId(), f.getCreatedAt()))
                .toList();
    }

    @Transactional
    public FavoriteDto add(User user, String rawPath) {
        Path resolved = pathAccessValidator.resolveWithinRoots(user, rawPath);
        MediaItem item = mediaItemService.findOrCreate(resolved);

        if (favoriteRepository.findByUserAndMediaItem(user, item).isPresent()) {
            throw new ConflictException("Este elemento ya está en tus favoritos");
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setMediaItem(item);
        Favorite saved = favoriteRepository.save(favorite);
        return toDto(user, item, saved.getId(), saved.getCreatedAt());
    }

    @Transactional
    public void remove(User user, String rawPath) {
        Path resolved = pathAccessValidator.resolveWithinRoots(user, rawPath);
        MediaItem item = mediaItemService.findOrCreate(resolved);
        favoriteRepository.deleteByUserAndMediaItem(user, item);
    }

    private FavoriteDto toDto(User user, MediaItem item, Long favoriteId, java.time.Instant createdAt) {
        String streamUrl = "/api/stream/" + streamTicketService.generate(user.getUsername(), item.getPath());
        MediaItemDto itemDto = new MediaItemDto(
                item.getId(),
                item.getTitle(),
                item.getPath(),
                item.getMediaType(),
                item.getContentType(),
                item.getSizeBytes(),
                item.getLastModified(),
                streamUrl);
        return new FavoriteDto(favoriteId, itemDto, createdAt);
    }
}