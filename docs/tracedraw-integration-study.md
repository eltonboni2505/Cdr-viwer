# Estudo de viabilidade: TraceDraw para Ver.cdr

Fonte analisada: https://github.com/tracedraw/tracedraw (2026-11-05)

## Diagnóstico do Ver.cdr
A implementação Android/Java atual lê somente entradas PNG de `previews/` nos CDR baseados em ZIP. Essas entradas não substituem a interpretação da estrutura vetorial e não garantem que cada PNG corresponda a uma página real. CDR RIFF antigo também não é suportado.

## Referências técnicas verificadas
- `crates/cdr-format/src/container.rs`: identificação de RIFF puro e ZIP que encapsula `content/riffData.cdr`, além de referências a dados externos.
- `crates/cdr/src/lib.rs`: `open_bytes()` produz modelo `Document` e relatório de parse; incompatibilidades são tratadas em modo best-effort.
- `crates/cdr/src/container.rs`: prévias podem estar em `previews/page1.png` ou `metadata/thumbnails/page1.bmp`.
- `crates/render/src/lib.rs`: rasterizador CPU baseado em tiny-skia, com opções de renderização por página, dimensões e anti-aliasing.
- `Cargo.toml`: workspace Rust, dependências em módulos cdr-format/cdr/core/render, projeto em estágio alpha.
- `LICENSE-MIT`, `LICENSE-APACHE` e `ATTRIBUTION.md`: licenças MIT OR Apache-2.0, manter avisos e atribuições quando reutilizar código.

## Plano de integração Android
1. Implementar testes com CDR reais (RIFF antigo, ZIP novo, multipágina, fontes ausentes, imagens e efeitos).
2. Prototipar núcleo Rust para abrir bytes de CDR e renderizar páginas em PNG/RGBA, utilizando exclusivamente componentes de leitura e desenho (sem incluir o editor de desktop).
3. Criar ponte Android JNI/NDK para bibliotecas arm64-v8a e, quando necessário, armeabi-v7a, com limites de memória e trabalho fora da thread principal.
4. Substituir gradualmente o carregador de prévias por renderização real, mantendo leitura das prévias como fallback.
5. Gerar miniaturas por página, zoom de alta resolução sob demanda e compartilhamento PNG correspondente à página renderizada.
6. Validar tamanhos de APK, consumo de memória, desempenho e segurança do parser com arquivos malformados.

## Limitações e riscos
- O TraceDraw é alpha: compatibilidade com todos os tipos de CDR não está garantida.
- O código é Rust; não pode ser simplesmente colado na aplicação Java. Necessita compilação cruzada Android e API JNI/FFI.
- Exportação/renderização de documentos complexos e fidelidade em fontes/efeitos exigem testes.
- A assinatura permanente de APK é um trabalho independente e necessário para atualizar sem conflitos.
- Não copiar marca, ícones ou quaisquer materiais de terceiros sem observar licenças.
- O uso como referência arquitetural não implica alteração de código nem incorporação automática.

## Critério de sucesso
Abrir arquivo CDR real e mostrar quantidade correta de páginas, renderizar conteúdo não dependente de `previews/`, permitir navegação e zoom sem distorcer as páginas, manter funcionalidade offline.
