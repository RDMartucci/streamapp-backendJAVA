package com.streamapp.streamappbackend.dto;

import java.util.List;

public record LibraryFilterOptionsDto(
        List<String> actors,
        List<String> directors,
        List<Integer> years,
        List<String> genres,
        List<Integer> stars
) {
}
