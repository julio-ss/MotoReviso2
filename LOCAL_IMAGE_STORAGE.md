# Armazenamento Local de Imagens de Veículos

## Solução Implementada ✅

Em vez de usar Firebase Storage, as imagens agora são salvas **localmente no dispositivo**.

### Como Funciona

```
Fluxo de Salvamento:
┌──────────────────────────────────────────────┐
│ 1. Usuário edita veículo                     │
│    → Clica "Alterar Foto"                    │
│    → Seleciona imagem da galeria             │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 2. Usuário clica "Salvar"                    │
│    → EditarVeiculoActivity.salvarAlteracoes()│
│    → Chama FirebaseManager.                  │
│      salvarImagemLocalmente()                │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 3. Imagem é copiada para o dispositivo       │
│    Localização:                              │
│    /data/data/br.jss.motoreviso/             │
│    files/imagens_veiculos/                   │
│    veiculo_ID_timestamp.jpg                  │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 4. Caminho completo é salvo no Firestore     │
│    veiculos/{id}                             │
│    ├─ marca: "Yamaha"                        │
│    ├─ modelo: "MT-07"                        │
│    └─ urlImagemPrincipal:                    │
│        "/data/data/br.jss.motoreviso/...jpg" │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│ 5. Dashboard carrega a imagem                │
│    → DashboardFragment.loadVehicleImage()    │
│    → Glide carrega do caminho local          │
│    → Imagem é exibida no card do veículo     │
└──────────────────────────────────────────────┘
```

## Estrutura de Diretórios

As imagens são salvas em:

```
/data/data/br.jss.motoreviso/
└── files/
    └── imagens_veiculos/
        ├── veiculo_FfAoAg2xkyx2lRVhbL4A_1780437466481.jpg
        ├── veiculo_ABC123_1780437500000.jpg
        └── ... mais imagens
```

Este diretório é:
- ✅ **Privado** - Só o app acessa
- ✅ **Persistente** - Não é limpado
- ✅ **Automático** - O sistema gerencia
- ✅ **Seguro** - Permissões gerenciadas pelo Android

## Mudanças no Código

### FirebaseManager.java

**Novo método:**
```java
public void salvarImagemLocalmente(Context context, Uri imageUri,
                                   String nomeArquivo, OnUploadCompleteListener callback)
```

**O que faz:**
1. Cria diretório `/imagens_veiculos/` se não existir
2. Copia a imagem do URI para esse diretório
3. Retorna o caminho completo via callback
4. Se houver erro, chama `onUploadFailed()`

### EditarVeiculoActivity.java

**Mudança:**
```java
// Antes: firebaseManager.uploadImagemVeiculo()  ❌
// Depois: firebaseManager.salvarImagemLocalmente()  ✅

firebaseManager.salvarImagemLocalmente(
    this,                  // Context
    imagemUri,            // Imagem selecionada
    nomeImagem,           // Nome do arquivo
    callback              // Listener
);
```

### DashboardFragment.java

**Sem mudanças necessárias!** ✅

O Glide já suporta caminhos locais:
```java
Glide.with(this)
    .load(urlImagemPrincipal)  // Funciona com caminho local também!
    .placeholder(R.drawable.ic_car_modern)
    .error(R.drawable.ic_car_modern)
    .centerCrop()
    .into(imgVeiculo);
```

## Vantagens

| Aspecto | Local | Firebase Storage |
|---------|-------|------------------|
| Configuração | ✅ Zero | ❌ Precisa plano |
| Custo | ✅ Grátis | ❌ Pago |
| Velocidade | ✅ Instantânea | ❌ Depende internet |
| Offline | ✅ Funciona | ❌ Não funciona |
| Espaço | ⚠️ Limitado (app) | ✅ Ilimitado |
| Sincronização | ❌ Só local | ✅ Na nuvem |

## Como Testar

### Passo 1: Reconstruir
```bash
Build → Clean Project
Build → Make Project
```

### Passo 2: Editar Veículo
1. Dashboard → Toque no veículo
2. Clique "Editar Veículo"
3. Clique "Alterar Foto"
4. Selecione imagem da galeria
5. Clique "Salvar"

### Passo 3: Verificar Logs

Você deve ver:
```
D  imagemUri is not null, saving image locally...
D  Salvando imagem localmente: veiculo_FfAoAg2xkyx2lRVhbL4A_...jpg
D  Imagem salva localmente em: /data/data/br.jss.motoreviso/files/imagens_veiculos/...jpg
D  Veículo atualizado com sucesso!
```

### Passo 4: Verificar no Dashboard
- Abra Dashboard
- A imagem deve aparecer no card do veículo
- Clique em outro veículo e volte
- A imagem ainda deve estar lá ✅

## Espaço em Disco

### Quanto espaço cada imagem usa?

- Imagem típica: **200-500 KB**
- Com 10 veículos: **2-5 MB** (espaço livre)
- Dispositivo moderno: **50-100+ GB** (espaço total)

**Não há problema de espaço!**

## Quando a Imagem é Deletada?

A imagem é deletada quando:
1. ✅ Usuário seleciona nova imagem (sobrescreve)
2. ✅ Usuário desinstala o app
3. ⚠️ Usuário limpa cache (se usar `getCacheDir()`)
4. ❌ Nunca é deletada automaticamente

**Nenhuma limpeza automática** - dados persistem!

## Permissões

O app **NÃO precisa** de novas permissões!

```xml
<!-- Já possui - não precisa mudar -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

A pasta `/files/` é gerenciada automaticamente pelo Android.

## Limitações

1. **Imagens não sincronizam entre dispositivos**
   - Se usuário trocar de telefone, precisa reimportar fotos

2. **Espaço limitado ao dispositivo**
   - Não há "nuvem" como Firebase Storage

3. **Não há backup automático**
   - Se deletar app, perde as imagens

**Solução**: Se precisar sincronizar entre dispositivos, adicionar Firebase Storage depois.

## Estrutura de Dados no Firestore

```
veiculos/{veiculoId}
├── marca: "Yamaha"
├── modelo: "MT-07"
├── placa: "ABC-1234"
├── kmAtual: 5000
├── urlImagemPrincipal: "/data/data/br.jss.motoreviso/files/imagens_veiculos/veiculo_FfAoAg2xkyx2lRVhbL4A_1780437466481.jpg"
└── ... outros campos
```

O Firestore armazena apenas o **caminho**, não a imagem.

## Troubleshooting

### Imagem não aparece no Dashboard

**Causa**: Caminho inválido ou imagem não foi salva
**Solução**:
1. Verificar logs para erros
2. Tentar salvar novamente
3. Verificar espaço em disco (Settings → Storage)

### Imagem desaparece ao fechar app

**Causa**: Erro no salvamento (não é normal)
**Solução**:
1. Verificar se há erro nos logs
2. Verificar permissões do app
3. Limpar dados do app e tentar novamente

### Erro: "Não foi possível abrir o arquivo de imagem"

**Causa**: Imagem selecionada está inacessível
**Solução**:
1. Tentar selecionar outra imagem
2. Verificar se galeria tem imagens
3. Reiniciar o app

## Git Commits

```
2de2005 fix(firebase): simplify storage path to avoid 404 errors
[novo] fix(image): save images locally instead of Firebase Storage
```

## Resumo

✅ **Imagens agora são salvas localmente no dispositivo**
- Sem necessidade de Firebase Storage
- Sem custos adicionais
- Mais rápido (sem internet)
- Simples de implementar
- Funciona offline

**Pronto para testar!** 🎉
