package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.fragments.VeiculosFragment;
import br.jss.motoreviso.activities.LoginActivity;
import br.jss.motoreviso.activities.MainActivity;
import br.jss.motoreviso.activities.SetupPinActivity;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.utils.BiometricHelper;
import br.jss.motoreviso.utils.SecurePreferencesManager;

public class ConfiguracoesFragment extends Fragment {

    private static final String PREF_APP = "app_prefs";
    private static final String KEY_NOTIFICACOES = "notificacoes_ativas";
    private static final String KEY_BACKUP = "backup_ativo";
    private static final String KEY_BACKUP_DATA = "backup_ultima_data";
    private static final String KEY_TEMA = "tema";           // dark | light | system
    private static final String KEY_BLOQUEIO = "bloqueio_min"; // 0 = nunca, 1, 5, 10, 30

    private SecurePreferencesManager securePrefs;
    private SharedPreferences appPrefs;
    private FirebaseAuth auth;

    // Views
    private TextView tvAvatarInicial, tvNome, tvEmail, tvMembroDesde;
    private TextView tvBloqueioValor, tvBackupStatus, tvTemaValor, tvQtdVeiculos;
    private SwitchMaterial switchBiometria, switchNotificacoes, switchBackup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracoes, container, false);

        auth = FirebaseAuth.getInstance();
        securePrefs = new SecurePreferencesManager(requireContext());
        appPrefs = requireContext().getSharedPreferences(PREF_APP, 0);

        inicializarViews(view);
        preencherPerfil();
        preencherSwitches();
        preencherValoresTexto();
        configurarCliques(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarQtdVeiculos();
    }

    // ─────────────────────────────────────────────────────────
    //  Inicialização
    // ─────────────────────────────────────────────────────────

    private void inicializarViews(View view) {
        tvAvatarInicial   = view.findViewById(R.id.tv_avatar_inicial);
        tvNome            = view.findViewById(R.id.tv_nome_usuario);
        tvEmail           = view.findViewById(R.id.tv_email_usuario);
        tvMembroDesde     = view.findViewById(R.id.tv_membro_desde);
        tvBloqueioValor   = view.findViewById(R.id.tv_bloqueio_valor);
        tvBackupStatus    = view.findViewById(R.id.tv_backup_status);
        tvTemaValor       = view.findViewById(R.id.tv_tema_valor);
        tvQtdVeiculos     = view.findViewById(R.id.tv_qtd_veiculos);
        switchBiometria   = view.findViewById(R.id.switch_biometria);
        switchNotificacoes= view.findViewById(R.id.switch_notificacoes);
        switchBackup      = view.findViewById(R.id.switch_backup);
    }

    // ─────────────────────────────────────────────────────────
    //  Preencher dados
    // ─────────────────────────────────────────────────────────

    private void preencherPerfil() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String nome = user.getDisplayName();
        String email = user.getEmail();

        if (nome == null || nome.trim().isEmpty()) {
            nome = email != null ? email.split("@")[0] : "Usuário";
        }

        tvNome.setText(nome);
        tvEmail.setText(email != null ? email : "");
        tvAvatarInicial.setText(String.valueOf(nome.charAt(0)).toUpperCase());

        // "Membro desde YEAR" — carrega qtd de veículos de forma assíncrona
        long criacao = user.getMetadata() != null ? user.getMetadata().getCreationTimestamp() : 0;
        Calendar cal = Calendar.getInstance();
        if (criacao > 0) cal.setTimeInMillis(criacao);
        int ano = cal.get(Calendar.YEAR);
        tvMembroDesde.setText("Membro desde " + ano);
    }

    private void carregarQtdVeiculos() {
        FirebaseManager.getInstance().obterTodosVeiculos()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    int n = snap != null ? snap.size() : 0;
                    String label = n + (n == 1 ? " veículo cadastrado" : " veículos cadastrados");
                    tvQtdVeiculos.setText(label);

                    // Atualiza também a linha do membro desde
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        long criacao = user.getMetadata() != null ? user.getMetadata().getCreationTimestamp() : 0;
                        Calendar cal = Calendar.getInstance();
                        if (criacao > 0) cal.setTimeInMillis(criacao);
                        int ano = cal.get(Calendar.YEAR);
                        String nomeTipo = n == 1 ? "moto" : "motos";
                        tvMembroDesde.setText("Membro desde " + ano + " · " + n + " " + nomeTipo);
                    }
                });
    }

    private void preencherSwitches() {
        boolean bioDisp = BiometricHelper.isBiometriaDisponivel(requireContext());
        switchBiometria.setEnabled(bioDisp);
        switchBiometria.setChecked(bioDisp && securePrefs.isBiometriaHabilitada());
        switchNotificacoes.setChecked(appPrefs.getBoolean(KEY_NOTIFICACOES, true));
        switchBackup.setChecked(appPrefs.getBoolean(KEY_BACKUP, false));
    }

    private void preencherValoresTexto() {
        // Bloqueio automático
        int bloqueioMin = appPrefs.getInt(KEY_BLOQUEIO, 5);
        tvBloqueioValor.setText(descricaoBloqueio(bloqueioMin));

        // Backup
        long ultimaData = appPrefs.getLong(KEY_BACKUP_DATA, 0);
        if (ultimaData > 0) {
            String dataStr = new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("pt", "BR"))
                    .format(new Date(ultimaData));
            tvBackupStatus.setText("Último: " + dataStr);
        } else {
            tvBackupStatus.setText("Nunca realizado");
        }

        // Tema
        String tema = appPrefs.getString(KEY_TEMA, "dark");
        tvTemaValor.setText(labelTema(tema));
    }

    // ─────────────────────────────────────────────────────────
    //  Cliques
    // ─────────────────────────────────────────────────────────

    private void configurarCliques(View view) {
        // Card de perfil → editar nome
        view.findViewById(R.id.card_perfil).setOnClickListener(v -> mostrarDialogEditarPerfil());

        // Biometria
        view.findViewById(R.id.row_biometria).setOnClickListener(v -> {
            if (!BiometricHelper.isBiometriaDisponivel(requireContext())) {
                Toast.makeText(requireContext(), "Biometria não disponível neste dispositivo", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean novoEstado = !switchBiometria.isChecked();
            switchBiometria.setChecked(novoEstado);
            securePrefs.habilitarBiometria(novoEstado);
            Toast.makeText(requireContext(),
                    novoEstado ? "Biometria ativada" : "Biometria desativada",
                    Toast.LENGTH_SHORT).show();
        });

        // Alterar PIN
        view.findViewById(R.id.row_alterar_pin).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SetupPinActivity.class)));

        // Bloqueio automático
        view.findViewById(R.id.row_bloqueio).setOnClickListener(v -> mostrarDialogBloqueio());

        // Notificações
        view.findViewById(R.id.row_notificacoes).setOnClickListener(v -> {
            boolean novoEstado = !switchNotificacoes.isChecked();
            switchNotificacoes.setChecked(novoEstado);
            appPrefs.edit().putBoolean(KEY_NOTIFICACOES, novoEstado).apply();
            Toast.makeText(requireContext(),
                    novoEstado ? "Notificações ativadas" : "Notificações desativadas",
                    Toast.LENGTH_SHORT).show();
        });

        // Backup automático
        view.findViewById(R.id.row_backup).setOnClickListener(v -> {
            boolean novoEstado = !switchBackup.isChecked();
            switchBackup.setChecked(novoEstado);
            appPrefs.edit().putBoolean(KEY_BACKUP, novoEstado).apply();
            if (novoEstado) {
                long agora = System.currentTimeMillis();
                appPrefs.edit().putLong(KEY_BACKUP_DATA, agora).apply();
                String dataStr = new SimpleDateFormat("dd/MM/yyyy, HH:mm", new Locale("pt", "BR"))
                        .format(new Date(agora));
                tvBackupStatus.setText("Último: " + dataStr);
                Toast.makeText(requireContext(), "Backup ativado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Backup desativado", Toast.LENGTH_SHORT).show();
            }
        });

        // Tema
        view.findViewById(R.id.row_tema).setOnClickListener(v -> mostrarDialogTema());

        // Editar perfil
        view.findViewById(R.id.row_editar_perfil).setOnClickListener(v -> mostrarDialogEditarPerfil());

        // Meus veículos
        view.findViewById(R.id.row_minhas_motos).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).carregarFragment(new VeiculosFragment());
            }
        });

        // Sair
        view.findViewById(R.id.row_sair).setOnClickListener(v -> confirmarSaida());
    }

    // ─────────────────────────────────────────────────────────
    //  Diálogos
    // ─────────────────────────────────────────────────────────

    private void mostrarDialogEditarPerfil() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String nomeAtual = user.getDisplayName();
        if (nomeAtual == null) nomeAtual = "";

        EditText edtNome = new EditText(requireContext());
        edtNome.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtNome.setText(nomeAtual);
        edtNome.setSelection(nomeAtual.length());
        edtNome.setHint("Seu nome");

        new AlertDialog.Builder(requireContext())
                .setTitle("Editar perfil")
                .setMessage("Como você quer ser chamado?")
                .setView(edtNome)
                .setPositiveButton("Salvar", (d, w) -> {
                    String novoNome = edtNome.getText().toString().trim();
                    if (novoNome.isEmpty()) {
                        Toast.makeText(requireContext(), "O nome não pode ser vazio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(novoNome)
                            .build();
                    user.updateProfile(req).addOnSuccessListener(v -> {
                        preencherPerfil();
                        Toast.makeText(requireContext(), "Perfil atualizado!", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogBloqueio() {
        String[] opcoes = {"1 minuto", "5 minutos", "10 minutos", "30 minutos", "Nunca"};
        int[] valores  = {1, 5, 10, 30, 0};
        int atual = appPrefs.getInt(KEY_BLOQUEIO, 5);

        int selecionado = 1; // default 5 min
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] == atual) { selecionado = i; break; }
        }
        final int[] escolha = {selecionado};

        new AlertDialog.Builder(requireContext())
                .setTitle("Bloqueio automático")
                .setSingleChoiceItems(opcoes, selecionado, (d, which) -> escolha[0] = which)
                .setPositiveButton("Aplicar", (d, w) -> {
                    int novoValor = valores[escolha[0]];
                    appPrefs.edit().putInt(KEY_BLOQUEIO, novoValor).apply();
                    tvBloqueioValor.setText(descricaoBloqueio(novoValor));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogTema() {
        String[] opcoes = {"Escuro", "Claro", "Sistema"};
        String[] valores = {"dark", "light", "system"};
        String atual = appPrefs.getString(KEY_TEMA, "dark");

        int selecionado = 0;
        for (int i = 0; i < valores.length; i++) {
            if (valores[i].equals(atual)) { selecionado = i; break; }
        }
        final int[] escolha = {selecionado};

        new AlertDialog.Builder(requireContext())
                .setTitle("Tema")
                .setSingleChoiceItems(opcoes, selecionado, (d, which) -> escolha[0] = which)
                .setPositiveButton("Aplicar", (d, w) -> {
                    String novoTema = valores[escolha[0]];
                    appPrefs.edit().putString(KEY_TEMA, novoTema).apply();
                    tvTemaValor.setText(labelTema(novoTema));
                    aplicarTema(novoTema);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarSaida() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Sair da conta")
                .setMessage("Deseja realmente sair?")
                .setPositiveButton("Sair", (d, w) -> {
                    auth.signOut();
                    securePrefs.setLoggedIn(false);
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────

    private void aplicarTema(String tema) {
        int mode;
        switch (tema) {
            case "light":  mode = AppCompatDelegate.MODE_NIGHT_NO;              break;
            case "system": mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;   break;
            default:       mode = AppCompatDelegate.MODE_NIGHT_YES;             break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private String descricaoBloqueio(int minutos) {
        if (minutos == 0) return "Nunca";
        if (minutos == 1) return "Após 1 min";
        return "Após " + minutos + " min";
    }

    private String labelTema(String tema) {
        switch (tema) {
            case "light":  return "Light";
            case "system": return "Auto";
            default:       return "Dark";
        }
    }
}
