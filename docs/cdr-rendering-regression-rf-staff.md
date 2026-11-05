# Regressão visual: RF PRODUÇÕES STAFF.cdr

## Evidência observada em 2026-11-05

O usuário comparou o mesmo CDR no CorelDRAW e no TraceDraw. **CorelDRAW é a referência visual**. No TraceDraw, as duas camisas aparecem praticamente pretas, enquanto no CorelDRAW há estampa turquesa com ondulações, mangas turquesa, logotipo RF frontal, texto STAFF, telefone, Instagram e outros detalhes. O fundo com marcas d'água e a faixa diagonal cinza inferior aparecem em ambos. **Não atribuir o problema da faixa inferior a transparências**.

## Arquivo de teste

Nome: `RF PRODUÇÕES STAFF.cdr` (fornecido pelo usuário no chat; não incluir o arquivo privado no repositório público sem autorização).

O contêiner ZIP contém `previews/thumbnail.png`, `previews/page1.png`, `previews/page2.png` e `previews/page3.png`. As imagens incorporadas são de baixa resolução e não servem como renderização final com zoom.

## Hipóteses a testar, não conclusões

1. Elementos ausentes por erro de parse (camadas, objetos, referências externas ou dados de bitmap).
2. Objetos lidos mas ocultados por ordem de composição, PowerClip, máscaras ou transparência.
3. Efeitos e mesclagem não suportados pelo renderizador.

## Critérios de aceite

- Exibir estampas turquesa e os elementos RF/STAFF e contatos corretamente, conforme referência CorelDRAW.
- Manter fundo e faixa diagonal inferior.
- Navegar nas três páginas reais; não confundir thumbnails ou resoluções alternativas com páginas.
- Permitir zoom em resolução maior que os PNGs embutidos.
- Quando o renderizador não conseguir reproduzir o conteúdo, informar limitação e oferecer prévia embutida, sem alegar fidelidade.

## Plano de diagnóstico

- Extrair relatório de parse do TraceDraw, inclusive warnings e contagem de objetos por página.
- Inspecionar árvores de objetos, máscaras, transparências, bitmaps e ordem de camadas.
- Comparar renderização com e sem efeitos complexos e registrar diferenças.
- Implementar teste de regressão visual com referência fornecida pelo usuário; evitar incorporar capturas privadas no GitHub público.
- Validar pipeline Android/Rust antes de prometer correção visual.

Referência de arquitetura: https://github.com/tracedraw/tracedraw
