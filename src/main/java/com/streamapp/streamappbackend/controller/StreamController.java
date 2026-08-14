package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.explorer.PathAccessValidator;
import com.streamapp.streamappbackend.service.streaming.StreamingAdapter;
import com.streamapp.streamappbackend.service.streaming.StreamTicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamTicketService streamTicketService;
    private final UserRepository userRepository;
    private final PathAccessValidator pathAccessValidator;
    private final StreamingAdapter streamingAdapter;

    public StreamController(StreamTicketService streamTicketService,
                            UserRepository userRepository,
                            PathAccessValidator pathAccessValidator,
                            StreamingAdapter streamingAdapter) {
        this.streamTicketService = streamTicketService;
        this.userRepository = userRepository;
        this.pathAccessValidator = pathAccessValidator;
        this.streamingAdapter = streamingAdapter;
    }

    /**
     * Sirve el archivo validando el ticket firmado. El ticket es la credencial
     * por sí mismo (no requiere header Authorization), por lo que funciona con
     * reproductor interno y externo (VLC).
     */
    @GetMapping("/{ticket}")
    public ResponseEntity<StreamingResponseBody> stream(@PathVariable("ticket") String ticket,
                                                        @RequestHeader(value = "Range", required = false) String rangeHeader) {
        StreamTicketService.Ticket t = streamTicketService.parse(ticket);

        User user = userRepository.findByUsername(t.username())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Path resolved = pathAccessValidator.resolveWithinRoots(user, t.path());

        if (!Files.isRegularFile(resolved)) {
            throw new NotFoundException("El archivo ya no existe: " + t.path());
        }

        return streamingAdapter.stream(resolved, rangeHeader);
    }
}