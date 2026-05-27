# Guia de Armazenamento de Imagens - MotoReviso

## 🎯 Nova Abordagem: Local Storage com Cache

A aplicação agora salva **apenas o caminho da imagem** no Firestore, sem fazer upload para Firebase Storage. Isso é mais eficiente, rápido e sem custos adicionais.

---

## 📱 Como Funciona

### 1. Quando o usuário seleciona uma imagem:
```
Usuário clica no FAB/botão
    ↓
Sistema abre galeria do dispositivo
    ↓
Usuário seleciona uma imagem
    ↓
URI da imagem é salva no banco de dados (Firestore)
    ↓
Imagem fica em cache automático
```

### 2. Quando a imagem é exibida:
```
App recupera o URI do Firestore
    ↓
Glide tenta carregar a imagem do caminho local
    ↓
Se encontrar: Mostra a imagem e faz cache
    ↓
Se não encontrar: Mostra imagem padrão (ic_car_modern)
```

---

## 💾 Armazenamento de Dados

### No Firestore (Documento do Veículo):
```json
{
  "marca": "Honda",
  "modelo": "CB 500",
  "placa": "ABC-1234",
  "urlImagemPrincipal": "content://media/external/images/media/12345",
  ...outros campos...
}
```

A URL é salva como uma string (caminho local da imagem).

---

## 🎨 Classe ImagemLoader

Criei uma classe auxiliar para centralizar o carregamento de imagens:

```java
// Carregar imagem com fallback automático
ImagemLoader.carregarImagem(context, imageView, imagemUri);

// Ou com drawable customizado
ImagemLoader.carregarImagemComFallback(
    context, 
    imageView, 
    imagemUri, 
    R.drawable.ic_custom
);
```

### Recursos:
- ✅ Carrega de URI local
- ✅ Cache automático do Glide
- ✅ Fallback para imagem padrão se não encontrar
- ✅ Tratamento de exceções
- ✅ Suporte a múltiplos formatos

---

## 📂 Estrutura de Pastas (Firesystem do Dispositivo)

As imagens ficam no diretório padrão do Android:
```
/storage/emulated/0/DCIM/Camera/
    └── moto_honda.jpg
    
/storage/emulated/0/Pictures/
    └── veiculo_photo.png
```

O caminho completo é salvo no Firestore.

---

## ⚙️ Implementação Técnica

### CadastroVeiculoActivity
```java
if (imagemUri != null) {
    veiculo.setUrlImagemPrincipal(imagemUri.toString());
}
salvarVeiculoNoFirebase(veiculo);
```

### EditarVeiculoActivity
```java
if (imagemUri != null) {
    veiculoAtual.setUrlImagemPrincipal(imagemUri.toString());
}
atualizarVeiculoFirebase();
```

### DetalheVeiculoActivity
```java
ImagemLoader.carregarImagem(this, imgVeiculo, veiculoAtual.getUrlImagemPrincipal());
```

---

## 🎯 Vantagens desta Abordagem

| Recurso | Local Storage | Firebase Storage |
|---------|---------------|------------------|
| Velocidade | ⚡⚡⚡ Muito rápido | ⚡⚡ Mais lento |
| Custo | 🎉 Grátis | 💰 Pago (depois de quota) |
| Cache | ✅ Automático | ✅ Automático |
| Dados | 📱 Dispositivo | ☁️ Cloud |
| Fallback | ✅ Imagem padrão | ✅ Imagem padrão |
| Sincronização | ❌ Não | ✅ Sim (mas caro) |

---

## 📸 Ciclo de Vida da Imagem

### 1. Seleção
- Usuário clica no botão de câmera/galeria
- Sistema abre picker nativo
- Usuário escolhe imagem

### 2. Salvamento
- URI da imagem é extraído: `content://media/...`
- Caminho é salvo no Firestore
- Imagem permanece no dispositivo (não é copiada)

### 3. Exibição
- App busca URI do Firestore
- Glide carrega a imagem do caminho local
- Cache automático evita recarregar

### 4. Caso a Imagem Seja Deletada
- Glide retorna erro
- ImagemLoader detecta e mostra fallback
- Usuário vê `ic_car_modern` (ícone padrão)

---

## 🚨 Limitações e Considerações

1. **Imagem em outro dispositivo**: Se a imagem foi de outro celular, não funcionará
2. **Imagem deletada**: Se o usuário apagar a imagem, a referência fica quebrada (mostra fallback)
3. **Backup**: As imagens não são sincronizadas com backup do Firebase
4. **Compartilhamento**: Não pode compartilhar para outros usuários

### ✅ Soluções:
- Se precisar sincronizar, pode migrar para Firebase Storage
- Para compartilhar, primeiro fazer upload e salvar URL público

---

## 📖 Exemplo de Uso

### Ao editar veículo:
```java
// EditarVeiculoActivity.java
fabAlterarFoto.setOnClickListener(v -> selecionarImagem());

private void selecionarImagem() {
    Intent intent = new Intent(Intent.ACTION_PICK, 
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    imagemLauncher.launch(intent);
}

// Callback recebe o URI
imagemUri = result.getData().getData();

// Ao salvar
if (imagemUri != null) {
    veiculoAtual.setUrlImagemPrincipal(imagemUri.toString());
}
atualizarVeiculoFirebase();
```

### Ao exibir veículo:
```java
// DetalheVeiculoActivity.java
ImagemLoader.carregarImagem(this, imgVeiculo, 
    veiculoAtual.getUrlImagemPrincipal());
```

---

## 🔄 Migração (Se Mudar de Ideia)

Se no futuro você quiser migrar para Firebase Storage:

1. Alteraria `FirebaseManager.uploadImagemVeiculo()` 
2. Voltaria a chamar upload em CadastroVeiculoActivity e EditarVeiculoActivity
3. `ImagemLoader` continuaria funcionando (Glide suporta URLs públicas)

---

## ✅ Checklist

- [x] Imagens salvam como caminho local
- [x] ImagemLoader gerencia carregamento
- [x] Cache automático ativado
- [x] Fallback para imagem padrão
- [x] Sem custo de armazenamento
- [x] Interface moderna com FAB

Pronto para usar! 🚀
