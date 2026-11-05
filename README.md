# Ver.cdr — visualizador Android

Visualizador experimental de arquivos CDR, PDF e imagens para Android, com interface escura e detalhes verdes.

## Recursos implementados no código

- Visualização em tela cheia, zoom por pinça (até 8×), duplo toque e deslocamento da imagem ampliada.
- Navegação entre páginas por deslize e miniaturas horizontais.
- Leitura de prévias PNG de página em arquivos CDR que utilizem o contêiner ZIP com entradas `previews/pageN.png`.
- Exibição de PDFs com múltiplas páginas (limite atual de 80 páginas por carregamento).
- Compartilhamento da página exibida em PNG e exportação das páginas carregadas como PNGs em um ZIP.
- Verificação de atualizações pelo gerenciador já existente.

## Limitações importantes

**O aplicativo ainda não renderiza o conteúdo vetorial CDR.** A visualização depende das prévias de página incorporadas no arquivo, que podem estar ausentes, incompletas, desatualizadas ou em baixa resolução. Em particular, transparências, PowerClip, fontes e outros efeitos complexos não estão garantidos. O zoom não aumenta a resolução real, e a exportação PNG preserva apenas a imagem carregada, não uma renderização vetorial de alta fidelidade.

O leitor atual reconhece apenas o padrão específico de entradas `previews/pageN.png` e não garante suporte a todos os formatos ou versões do CorelDRAW. Não confundir a quantidade de prévias encontradas com a quantidade real de páginas do documento.

As páginas são mantidas em memória; arquivos grandes podem consumir muita RAM. O carregamento e a exportação ainda precisam ser movidos para tarefas em segundo plano e testados em aparelhos reais.

## Próximos passos

1. Validar o build e os gestos em dispositivo Android.
2. Testar CDRs reais de várias versões e comparar com o CorelDRAW, inclusive o caso RF PRODUÇÕES STAFF.
3. Investigar um renderizador CDR nativo compatível, sem incorporar bibliotecas proprietárias do CorelDRAW.
4. Adicionar paginação e cache para arquivos grandes, além de exportação em segundo plano.
5. Incorporar a arte oficial da logo Ver.cdr aos recursos de ícone do Android.

A compilação do projeto é realizada pelo fluxo GitHub Actions configurado no repositório.
