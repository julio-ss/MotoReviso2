# Dashboard Moderno — Documentação

## 📊 Visão Geral

A nova tela de Dashboard apresenta um design moderno baseado no Moto Reviso TFT Design System, com:

- ✅ Saudação dinâmica (Bom dia/Boa tarde/Boa noite)
- ✅ Card do veículo principal com imagem
- ✅ Progresso visual para próxima revisão
- ✅ Resumo de stats (KM mês, consumo, custo)
- ✅ Último trajeto registrado
- ✅ Integração com dados reais do Firebase

---

## 🎨 Componentes

### 1. Header
- Saudação dinâmica baseada na hora do dia
- Nome do usuário
- Botão de notificações

### 2. Vehicle Card
- Imagem do veículo (via Firebase/Glide)
- Nome do modelo
- KM atual
- Badge "Principal"
- Clicável → abre detalhes do veículo

### 3. Revision Progress Card
- Anel de progresso circular (35% do exemplo)
- KM restantes para próxima revisão
- Tipo de revisão
- Botão "Agendar"
- Clicável → abre tela de rastreamento

### 4. Stats Row (3 cards)
- KM rodados este mês
- Consumo médio (km/l)
- Custo do mês
- Cada um com ícone representativo

### 5. Last Trip Card
- Nome do trajeto
- Data
- Distância
- Velocidade máxima
- Tempo decorrido

---

## 🔌 Integração com Firebase

O Fragment carrega dados via `FirebaseManager`:

```java
firebaseManager.carregarVeiculoPrincipal(new FirebaseManager.VeiculoCallback() {
    @Override
    public void onSuccess(Veiculo veiculo) {
        updateVeiculoCard(veiculo);
        updateRevisionCard(veiculo);
        loadStats(veiculo.getId());
        loadLastTrip(veiculo.getId());
    }
});
```

### Dados Usados

| Campo | Fonte | Exibição |
|-------|-------|----------|
| `marca` + `modelo` | Veiculo | Header do card |
| `kmAtual` | Veiculo | KM atual |
| `urlImagemPrincipal` | Veiculo | Imagem background |
| `getKmParaProximaRevisao()` | Veiculo (método) | Progress + texto |
| `intervaloRevisao` | Veiculo | Cálculo de progresso |
| Trajetos | Firebase query | Último trajeto |

---

## 📝 Como Usar no Seu Projeto

### Opção 1: Substituir Fragment Existente
Se você tem um fragment que é o primeiro da BottomNavigationView:

1. Mude `android:name="...VeiculosFragment"` para `...DashboardFragment`

### Opção 2: Adicionar como Nova Aba
1. Adicione item ao menu de navegação
2. Crie um novo fragmento que instancia DashboardFragment

### Opção 3: Activity Principal
Se quer Dashboard como screen principal:

```java
FragmentManager fm = getSupportFragmentManager();
fm.beginTransaction()
    .replace(R.id.fragment_container, new DashboardFragment())
    .commit();
```

---

## 🎯 Campos Customizáveis

Todos estes podem ser ajustados no Fragment:

```java
textGreeting.setText(hora);              // Saudação
textVeiculoNome.setText(...);            // Nome do veículo
textKmAtual.setText(...);                // KM
textProxRevisao.setText(...);            // KM para revisão
textKmMes.setText(...);                  // Stats
```

---

## 🔧 Melhorias Futuras

- [ ] Health cluster (óleo, pneus, freios, corrente) — adicionar progresso das saúdes
- [ ] Charts de consumo/KM histórico
- [ ] Notificações inline (alertas de manutenção)
- [ ] Modo light/dark automático
- [ ] Swipe para trocar entre veículos
- [ ] Sync offline → online quando conectar

---

## 📱 Responsividade

O layout usa:
- `NestedScrollView` para conteúdo scrollável
- `LinearLayout` com pesos para distribuição
- `MaterialCardView` com bordas modernas
- Espaçamento consistente (16dp padrão)

Testado em:
- ✅ Phones 5.5"
- ✅ Tablets 7" e 10"
- ✅ Dark mode
- ✅ Landscape

---

## 🚀 Navegação

Das seguintes telas você pode ir para Dashboard:
- Clique no vehicle card → `DetalheVeiculoActivity`
- Clique no revision card → `RastreamentoActivity`
- Clique no trip card → Abre detalhe do trajeto (a implementar)

---

## 📊 Dados Mocados vs. Reais

Atualmente mocados (para exemplo):
- Custo mês: `R$ 150,00`
- KM mês: `320 km`
- Consumo: `22.5 km/l`

Para integrar com dados reais:
```java
// 1. Carregar stats do Firestore
firebaseManager.carregarStatsMes(veiculoId, callback);

// 2. Calcular consumo a partir de trajetos
float consumo = calcularConsumo(trajetos);

// 3. Atualizar TextViews
textConsumo.setText(String.format("%.1f km/l", consumo));
```

---

## 🎨 Design System

Usa tokens do novo design:
- **Cores**: `@color/background`, `@color/secondary`, `@color/text_primary`
- **Drawables**: `bg_card_surface`, `bg_pill_accent_dim`, `gradient_overlay`
- **Tipo**: `text_primary` (20sp bold para headlines)
- **Espaçamento**: 16dp padrão, 13dp gaps entre cards

---

## ✅ Checklist de Implementação

- [x] Layout XML moderno
- [x] Fragment com lógica de carregamento
- [x] Integração Firebase
- [x] Navegação para telas relacionadas
- [ ] Load stats reais (customizar conforme necessário)
- [ ] Health indicators (opcional)
- [ ] Refresh pull-to-refresh (opcional)
- [ ] Share trajeto (opcional)

---

## 🐛 Debug

Para ver logs de carregamento:
```bash
adb logcat | grep Dashboard
```

Para mockar dados:
```java
// No Fragment
private void mockData() {
    textVeiculoNome.setText("Yamaha MT-07");
    textKmAtual.setText("12.450 km");
    textProxRevisao.setText("1.550 km restantes");
}
```

---

**Versão**: 1.0  
**Data**: 02/06/2026  
**Branch**: `modern-ui-refactor`
