# UI Migration Report — MotoReviso Modern Dark Design

**Branch:** `modern-ui-refactor`  
**Data:** 02/06/2026  
**Base design:** MotoReviso TFT Design System (React mockup → Android XML)

---

## Design System Aplicado

| Token | Valor |
|-------|-------|
| Background | `#0B0C0E` |
| Surface | `#15171A` |
| Surface Variant | `#1C1F23` |
| Accent / Secondary | `#00A8FF` |
| Accent Deep | `#0066CC` |
| Success | `#1FD68B` |
| Warning | `#FFB020` |
| Danger / Error | `#FF4D4D` |
| Text Primary | `#F2F5F7` |
| Text Secondary | `#98A0A8` |
| Text Faint | `#5A626B` |
| Card radius | `18dp` |
| Button radius | `14dp` |

---

## Telas Migradas

| Layout | Tela | Mudanças-chave |
|--------|------|----------------|
| `activity_login.xml` | Login / Desbloqueio | Botão biométrico circular grande, teclado PIN 3×4 customizado, logo com gradiente accent |
| `activity_main.xml` | Shell principal | Bottom nav com fundo arredondado (#15171A), seletor de cor por estado |
| `activity_detalhe_veiculo.xml` | Detalhe do Veículo | Hero image 220dp com gradient overlay, info sobreposta, tab bar scrollável |
| `activity_cadastro_veiculo.xml` | Cadastro de Veículo | Formulário com TextInputLayout OutlinedBox, fundo surface_variant |
| `activity_editar_veiculo.xml` | Edição de Veículo | Mesmas mudanças de formulário + card de foto |
| `activity_cadastro_manutencao.xml` | Cadastro de Manutenção | Campos modernizados, cards de agendamento |
| `activity_detalhe_manutencao.xml` | Detalhe de Manutenção | Cards informativos com bordas status-aware |
| `activity_galeria_veiculo.xml` | Galeria do Veículo | Grid de imagens com cards arredondados |
| `activity_rastreamento.xml` | Rastreamento GPS | Velocímetro grande (52sp), stats compactas, mapa em card |
| `activity_map_trajeto.xml` | Mapa do Trajeto | Overlay de stats sobre mapa full-screen |
| `activity_setup_pin.xml` | Configurar PIN | Teclado PIN visual, dots indicadores |
| `dialog_compartilhamento.xml` | Compartilhamento | Dialog com card arredondado, botões outline |
| `fragment_veiculos.xml` | Lista de Veículos (Garagem) | Header com subtítulo dinâmico, empty state com ícone |
| `fragment_manutencoes.xml` | Lista de Manutenções | Chips de filtro horizontal scrollável |
| `fragment_painel_manutencoes.xml` | Painel de Manutenções | Cards de status (warn/accent), spinners estilizados |
| `fragment_configuracoes.xml` | Configurações | Card de perfil com accent border, grupos de configurações |
| `fragment_especificacoes.xml` | Especificações do Veículo | Rows com label/valor separados por divider |
| `fragment_imagens_veiculo.xml` | Fotos do Veículo | Grid 2 colunas com FAB de adicionar |
| `fragment_manutencoes_veiculo.xml` | Manutenções do Veículo | Timeline com card items |
| `fragment_trajetos.xml` | Lista de Trajetos | Cards com stats de distância/tempo/velocidade |
| `fragment_trajetos_veiculo.xml` | Trajetos do Veículo | Idem com filtro por veículo |
| `item_veiculo.xml` | Item da lista de veículos | Imagem 90×90 arredondada + info + badge de alerta |
| `item_manutencao.xml` | Item de manutenção simples | Card compacto com status badge e data |
| `item_manutencao_avancado.xml` | Item de manutenção avançado | Card expandido com KM, custo, menu contextual |
| `item_trajeto.xml` | Item de trajeto | Card com stats em linha (dist, vel máx, tempo) |
| `item_imagem.xml` | Item da galeria | Card de imagem com overlay de nome |

---

## Componentes Criados

### Drawables (20 arquivos)
| Arquivo | Uso |
|---------|-----|
| `bg_card_surface.xml` | Fundo de card com borda sutil e raio 18dp |
| `bg_pill_accent.xml` | Badge/chip com cor accent sólida |
| `bg_pill_accent_dim.xml` | Badge/chip com cor accent translúcida |
| `bg_status_ok.xml` | Background badge de sucesso (verde dim) |
| `bg_status_warn.xml` | Background badge de atenção (laranja dim) |
| `bg_status_danger.xml` | Background badge de erro (vermelho dim) |
| `bg_icon_container.xml` | Container quadrado para ícones (surface_variant) |
| `bg_icon_accent.xml` | Container quadrado para ícones (accent dim) |
| `bg_biometric_button.xml` | Círculo para botão biométrico (selector com estado pressionado) |
| `bg_pin_key.xml` | Tecla do teclado PIN (selector com estado pressionado) |
| `bg_pin_dot_empty.xml` | Bolinha vazia do indicador de PIN |
| `bg_pin_dot_filled.xml` | Bolinha preenchida do indicador de PIN |
| `bg_bottom_nav.xml` | Fundo da bottom nav com cantos superiores arredondados |
| `bg_surface_variant.xml` | Fundo alternativo de surface com raio 14dp |
| `bg_progress_track.xml` | Trilho da barra de progresso |
| `bg_progress_fill_accent.xml` | Preenchimento da barra de progresso |
| `bg_fab.xml` | Fundo do FAB com gradiente accent |
| `bg_icon_logo.xml` | Container do logo na tela de login (gradiente accent, raio 22dp) |
| `bg_overlay_button.xml` | Botão com fundo escuro semi-transparente (sobre imagens) |
| `bg_dot_green.xml` | Indicador circular de GPS ativo |

### Seletores de Cor
| Arquivo | Uso |
|---------|-----|
| `res/color/nav_item_color.xml` | Tinta dos ícones da bottom nav (accent quando ativo, secondary quando inativo) |

### Valores atualizados
- `colors.xml` — paleta completa nova com 30+ tokens
- `styles.xml` — tema, botão, card, input, TextAppearance modernizados
- `dimens.xml` — raios e espaçamentos atualizados

---

## Componentes Removidos

Nenhum arquivo foi removido. Layouts obsoletos foram reescritos in-place. Todos os IDs de views foram preservados para compatibilidade com o código Java existente.

---

## Ajustes Realizados

1. **Paleta de cores**: migrada de roxo-escuro (#1F1F2E) para preto TFT (#0B0C0E) com accent azul elétrico (#00A8FF)
2. **Cards sem elevação**: substituídos por cards com `elevation=0` e borda sutil (`strokeWidth=1dp`)
3. **Raio de canto**: padronizado em 18dp para cards e 14dp para botões/inputs
4. **Status bar e nav bar**: atualizados para `@color/background` (#0B0C0E)
5. **ProgressBar**: tinte aplicado em todos os spinners de carregamento
6. **Bottom navigation**: fundo atualizado para surface com cantos arredondados no topo
7. **Tela de login**: totalmente redesenhada com biometria visual como ponto focal
8. **Rastreamento**: velocímetro expandido para 52sp com layout de duas colunas de stats
9. **Listas**: padding bottom adicionado para não cobrir conteúdo com FAB/nav bar
10. **Empty states**: adicionados ícone + texto em todas as listas

---

## Possíveis Melhorias Futuras

1. **Fontes custom**: integrar Saira Condensed e Rajdhani via `res/font/` XML (Google Fonts Downloadable Fonts API) para alinhar 100% com o mockup TFT
2. **WindowInsets**: usar `WindowInsetsController` no Java para ajustar padding das views conforme a barra de status/nav bar
3. **Transições**: usar `MotionLayout` na tela de login para animar a troca entre modo biométrico e PIN
4. **Shimmer loading**: substituir `ProgressBar` por efeito shimmer nos RecyclerViews enquanto carregam
5. **Dark Mode explícito**: adicionar `res/values-night/colors.xml` para suporte oficial ao sistema de temas do Android
6. **Splash screen**: criar uma splash screen moderna usando a API `SplashScreen` (Android 12+) com o logo animado
7. **Speedometer custom view**: criar uma `CustomView` com canvas para desenhar o velocímetro em arco como no mockup
8. **Health bars**: criar barras de progresso customizadas com gradiente de cor baseado no valor (ok→warn→danger)
9. **Glow effects**: para dispositivos com Android 12+, usar `RenderEffect` para criar o brilho (glow) do accent nos cards principais
10. **Haptic feedback**: adicionar feedback tátil nas teclas do PIN e no botão biométrico

---

*Relatório gerado automaticamente ao final da migração UI.*
