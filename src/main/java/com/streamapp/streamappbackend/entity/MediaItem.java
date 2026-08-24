package com.streamapp.streamappbackend.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Representa un elemento de media (audio/video) dentro del catálogo.
 * Cuando la fuente es LOCAL, almacena la ruta relativa dentro de media.root.
 * El modelo queda preparado para fuentes remotas (por URL) en fases futuras.
 */
@Entity
@Table(name = "media_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_media_items_path", columnNames = "path")
})
public class MediaItem {

    public enum SourceType { LOCAL, REMOTE }

    public enum MediaType { AUDIO, VIDEO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MediaType mediaType;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(name = "last_modified")
    private Instant lastModified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "title_original")
    private String titleOriginal;

    @Column(name = "year")
    private Integer year;

    @Column(name = "genres")
    private String genres;  // comma-separated

    @Column(name = "cast")
    private String cast;  // top billed actors

    @Column(name = "directors")
    private String directors;

    @Column(name = "trailer_youtube_id")
    private String trailerYouTubeId;

    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(name = "media_type_detail")
    private String mediaTypeDetail; // "movie" or "series"

    @Column(name = "overview")
    private String overview;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getGenres() {
        return genres;
    }

    public void setGenres(String genres) {
        this.genres = genres;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getDirectors() {
        return directors;
    }

    public void setDirectors(String directors) {
        this.directors = directors;
    }

    public String getTrailerYouTubeId() {
        return trailerYouTubeId;
    }

    public void setTrailerYouTubeId(String trailerYouTubeId) {
        this.trailerYouTubeId = trailerYouTubeId;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getMediaTypeDetail() {
        return mediaTypeDetail;
    }

    public void setMediaTypeDetail(String mediaTypeDetail) {
        this.mediaTypeDetail = mediaTypeDetail;
    }
}