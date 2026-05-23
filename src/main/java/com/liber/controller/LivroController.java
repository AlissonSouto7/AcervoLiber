package com.liber.controller;

import com.liber.dto.CapaResponse;
import com.liber.dto.LivroRequest;
import com.liber.dto.LivroResponse;
import com.liber.entity.LivroCapa;
import com.liber.service.LivroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/livros")
@RequiredArgsConstructor
@Tag(name = "Livros", description = "Catalogo de livros")
public class LivroController {

    private final LivroService livroService;

    @GetMapping
    @Operation(summary = "Lista livros com paginacao e busca")
    public Page<LivroResponse> listar(
            @Parameter(description = "Busca em titulo, autor ou ISBN")
            @RequestParam(required = false) String termo,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return livroService.listar(termo, pageable);
    }

    @GetMapping("/capa")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Resolve a URL da capa por ISBN ou titulo+autor (preview do formulario)")
    public CapaResponse resolverCapa(
            @Parameter(description = "ISBN do livro (com ou sem hifens)")
            @RequestParam(required = false) String isbn,
            @Parameter(description = "Titulo — usado quando nao ha ISBN ou o ISBN nao tem capa")
            @RequestParam(required = false) String titulo,
            @Parameter(description = "Autor — usado junto do titulo")
            @RequestParam(required = false) String autor) {
        return new CapaResponse(livroService.resolverCapa(isbn, titulo, autor));
    }

    @PostMapping(value = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Envia uma capa propria para o livro (substitui a capa automatica)")
    public LivroResponse enviarCapa(@PathVariable Long id,
                                    @RequestParam("file") MultipartFile file) throws IOException {
        return livroService.enviarCapa(id, file.getBytes(), file.getContentType());
    }

    @DeleteMapping("/{id}/capa")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Remove a capa manual e volta para a capa automatica")
    public LivroResponse removerCapa(@PathVariable Long id) {
        return livroService.removerCapaManual(id);
    }

    @GetMapping("/{id}/capa-imagem")
    @Operation(summary = "Serve a imagem da capa enviada manualmente (publico, para <img>)")
    public ResponseEntity<byte[]> capaImagem(@PathVariable Long id) {
        LivroCapa capa = livroService.lerCapaImagem(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(capa.getContentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
            .body(capa.getImagem());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca livro por id")
    public LivroResponse buscar(@PathVariable Long id) {
        return livroService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Cadastra um novo livro")
    public ResponseEntity<LivroResponse> cadastrar(@Valid @RequestBody LivroRequest req,
                                                   UriComponentsBuilder uri) {
        LivroResponse criado = livroService.cadastrar(req);
        return ResponseEntity
            .created(uri.path("/api/v1/livros/{id}").buildAndExpand(criado.id()).toUri())
            .body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Atualiza um livro existente")
    public LivroResponse atualizar(@PathVariable Long id, @Valid @RequestBody LivroRequest req) {
        return livroService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BIBLIOTECARIO','ADMIN')")
    @Operation(summary = "Remove um livro (apenas se nao tiver historico de emprestimos)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        livroService.remover(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
