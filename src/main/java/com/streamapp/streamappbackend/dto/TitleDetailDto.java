package com.streamapp.streamappbackend.dto;

public record TitleDetailDto(
        String title,
        String titleOriginal,
        Integer year,
        String genres,
        String overview,
        String cast,
        String directors,
        String trailerUrl,
        String posterUrl,
        String mediaType,
        Long tmdbId,
        Double voteAverage,
        java.util.List<SeasonDto> seasons,
        Integer runtime,
        String countries,
        String studios,
        String releaseDate,
        String backdropUrl
) {}
