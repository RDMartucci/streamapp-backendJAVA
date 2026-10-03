package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosterControllerTest {

    @Mock
    private MediaItemRepository mediaItemRepository;

    @InjectMocks
    private PosterController posterController;

    @Test
    void sharesSelectedTmdbMetadataWithCopiesOfSameMediaType() {
        MediaItem selected = mediaItem(1L, 1726L, "movie");
        MediaItem copy = mediaItem(2L, 1726L, "movie");
        when(mediaItemRepository.findByPath("F:/media/Iron Man.mp4"))
                .thenReturn(Optional.of(selected));
        when(mediaItemRepository.findAllByTmdbIdAndMediaTypeDetail(1726L, "movie"))
                .thenReturn(List.of(selected, copy));

        var response = posterController.setPosterByPath(null, Map.of(
                "path", "F:/media/Iron Man.mp4",
                "posterUrl", "https://image.tmdb.org/t/p/w342/iron-man.jpg",
                "tmdbId", "1726",
                "titleOriginal", "Iron Man",
                "year", "2008",
                "genres", "Action, Science Fiction",
                "overview", "A synopsis longer than a title.",
                "mediaType", "movie",
                "metadataProvider", "tmdb"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("https://image.tmdb.org/t/p/w342/iron-man.jpg", copy.getPosterUrl());
        assertEquals("Iron Man", copy.getTitleOriginal());
        assertEquals(2008, copy.getYear());
        assertEquals("Action, Science Fiction", copy.getGenres());
        verify(mediaItemRepository).findAllByTmdbIdAndMediaTypeDetail(1726L, "movie");
    }

    private MediaItem mediaItem(Long id, Long tmdbId, String mediaTypeDetail) {
        MediaItem item = new MediaItem();
        item.setId(id);
        item.setTmdbId(tmdbId);
        item.setMediaTypeDetail(mediaTypeDetail);
        return item;
    }
}