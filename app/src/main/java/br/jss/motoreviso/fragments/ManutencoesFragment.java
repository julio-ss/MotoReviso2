package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroManutencaoActivity;
import br.jss.motoreviso.adapters.ManutencaoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Veiculo;

public class ManutencoesFragment extends Fragment {

    // Tabs
    private MaterialButton tabPainel, tabHistorico;
    private NestedScrollView scrollPainel;
    private FrameLayout frameHistorico;

    // Painel views
    private TextView tvPendentes, tvVencidas;
    private TextView tvCustoMes, tvCustoAno;
    private LinearLayout barChartContainer;
    private LinearLayout proximasTrocasContainer;
    private TextView tvSemTrocas;

    // Histórico views
    private RecyclerView recyclerView;
    private TextView textVazio;
    private ProgressBar progressBar;

    // Data
    private final List<Veiculo> veiculos = new ArrayList<>();
    private final List<Manutencao> manutencoes = new ArrayList<>();
    private ManutencaoAdapter adapter;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manutencoes, container, false);

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews(view);
        setupTabs();
        setupRecyclerView();
        setupFab(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarDados();
    }

    private void inicializarViews(View view) {
        tabPainel = view.findViewById(R.id.tab_painel);
        tabHistorico = view.findViewById(R.id.tab_historico);
        scrollPainel = view.findViewById(R.id.scroll_painel);
        frameHistorico = view.findViewById(R.id.frame_historico);

        tvPendentes = view.findViewById(R.id.tv_pendentes);
        tvVencidas = view.findViewById(R.id.tv_vencidas);
        tvCustoMes = view.findViewById(R.id.tv_custo_mes);
        tvCustoAno = view.findViewById(R.id.tv_custo_ano);
        barChartContainer = view.findViewById(R.id.bar_chart_container);
        proximasTrocasContainer = view.findViewById(R.id.proximas_trocas_container);
        tvSemTrocas = view.findViewById(R.id.tv_sem_trocas);

        recyclerView = view.findViewById(R.id.recycler_manutencoes);
        textVazio = view.findViewById(R.id.text_vazio);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupTabs() {
        tabPainel.setOnClickListener(v -> mostrarAba(true));
        tabHistorico.setOnClickListener(v -> mostrarAba(false));
        mostrarAba(true);
    }

    private void mostrarAba(boolean painel) {
        scrollPainel.setVisibility(painel ? View.VISIBLE : View.GONE);
        frameHistorico.setVisibility(painel ? View.GONE : View.VISIBLE);

        // Tab ativo: fundo secondary, texto branco
        tabPainel.setBackgroundTintList(ColorStateList.valueOf(
                painel ? getResources().getColor(R.color.secondary) : getResources().getColor(R.color.transparent)));
        tabPainel.setTextColor(painel ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_secondary));

        tabHistorico.setBackgroundTintList(ColorStateList.valueOf(
                !painel ? getResources().getColor(R.color.secondary) : getResources().getColor(R.color.transparent)));
        tabHistorico.setTextColor(!painel ? getResources().getColor(R.color.white) : getResources().getColor(R.color.text_secondary));
    }

    private void setupRecyclerView() {
        adapter = new ManutencaoAdapter(manutencoes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab(View view) {
        ExtendedFloatingActionButton fab = view.findViewById(R.id.fab_registrar);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CadastroManutencaoActivity.class);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Carregamento de dados
    // ─────────────────────────────────────────────────────────────────

    private void carregarDados() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterTodosVeiculos().addOnCompleteListener(taskV -> {
            if (!isAdded()) return;

            veiculos.clear();
            if (taskV.isSuccessful() && taskV.getResult() != null) {
                for (DocumentSnapshot doc : taskV.getResult().getDocuments()) {
                    Veiculo v = doc.toObject(Veiculo.class);
                    if (v != null) { v.setId(doc.getId()); veiculos.add(v); }
                }
            }

            firebaseManager.obterTodasManutencoes().addOnCompleteListener(taskM -> {
                if (!isAdded()) return;

                if (progressBar != null) progressBar.setVisibility(View.GONE);

                manutencoes.clear();
                if (taskM.isSuccessful() && taskM.getResult() != null) {
                    for (DocumentSnapshot doc : taskM.getResult().getDocuments()) {
                        Manutencao m = doc.toObject(Manutencao.class);
                        if (m != null) { m.setId(doc.getId()); manutencoes.add(m); }
                    }
                }

                renderizarPainel();

                adapter.notifyDataSetChanged();
                if (textVazio != null) {
                    textVazio.setVisibility(manutencoes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────
    //  Renderização do Painel
    // ─────────────────────────────────────────────────────────────────

    private void renderizarPainel() {
        calcularRevisoes();
        calcularCustos();
        renderizarProximasTrocas();
    }

    private void calcularRevisoes() {
        int pendentes = 0, vencidas = 0;
        for (Veiculo v : veiculos) {
            if (v.getKmAtual() == null || v.getKmTroca() == null) continue;
            long kmAtual = v.getKmAtual();
            long interval = v.getIntervaloRevisao() != null ? v.getIntervaloRevisao() : 5000L;
            long proxKm = v.getKmTroca() + interval;
            if (kmAtual >= proxKm) {
                vencidas++;
            } else if (kmAtual >= proxKm - 500) {
                pendentes++;
            }
        }
        tvPendentes.setText(String.valueOf(pendentes));
        tvVencidas.setText(String.valueOf(vencidas));
    }

    private void calcularCustos() {
        Calendar cal = Calendar.getInstance();
        int mesAtual = cal.get(Calendar.MONTH);   // 0-based
        int anoAtual = cal.get(Calendar.YEAR);

        float[] custos6Meses = new float[6];
        float custoTotal = 0f, custoMes = 0f;

        for (Manutencao m : manutencoes) {
            if (m.getCusto() == null || m.getDataRevisao() == null) continue;
            Calendar mCal = Calendar.getInstance();
            mCal.setTimeInMillis(m.getDataRevisao());
            int mMes = mCal.get(Calendar.MONTH);
            int mAno = mCal.get(Calendar.YEAR);

            if (mAno == anoAtual) {
                custoTotal += m.getCusto();
                if (mMes == mesAtual) custoMes += m.getCusto();

                // Índice no array: 5 = mês atual, 0 = 5 meses atrás
                int idx = 5 - (mesAtual - mMes + 12) % 12;
                if (idx >= 0 && idx < 6) {
                    custos6Meses[idx] += m.getCusto();
                }
            }
        }

        tvCustoMes.setText(formatarMoeda(custoMes));
        tvCustoAno.setText(formatarMoeda(custoTotal));

        renderizarGrafico(custos6Meses, mesAtual);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Gráfico de barras
    // ─────────────────────────────────────────────────────────────────

    private void renderizarGrafico(float[] valores, int mesAtual) {
        barChartContainer.removeAllViews();

        float maxValor = 0;
        for (float v : valores) if (v > maxValor) maxValor = v;
        if (maxValor == 0) maxValor = 1;

        String[] mesesNomes = calcularNomesMeses(mesAtual);
        int corAtivo = getResources().getColor(R.color.secondary);
        int corInativo = getResources().getColor(R.color.surface3);
        int corTexto = getResources().getColor(R.color.text_primary);
        int corTextoFraco = getResources().getColor(R.color.text_tertiary);
        int alturaMaxBarraPx = dpToPx(60);

        for (int i = 0; i < 6; i++) {
            float valor = valores[i];
            boolean ativo = (i == 5); // índice 5 = mês atual

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, dpToPx(80), 1f));

            // Área das barras (alinhamento bottom)
            LinearLayout areaBarras = new LinearLayout(requireContext());
            areaBarras.setOrientation(LinearLayout.VERTICAL);
            areaBarras.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            areaBarras.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            // Label de valor acima da barra (só para o mês ativo)
            if (ativo && valor > 0) {
                TextView tvValor = new TextView(requireContext());
                tvValor.setText(formatarValorCurto(valor));
                tvValor.setTextSize(10f);
                tvValor.setTextColor(corTexto);
                tvValor.setTypeface(null, Typeface.BOLD);
                tvValor.setGravity(Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vlp.bottomMargin = dpToPx(3);
                tvValor.setLayoutParams(vlp);
                areaBarras.addView(tvValor);
            }

            // Barra
            int altBarraPx = (int) (valor / maxValor * alturaMaxBarraPx);
            altBarraPx = Math.max(altBarraPx, dpToPx(4));

            View barra = new View(requireContext());
            GradientDrawable bgBarra = new GradientDrawable();
            bgBarra.setShape(GradientDrawable.RECTANGLE);
            bgBarra.setCornerRadii(new float[]{dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4), 0, 0, 0, 0});
            bgBarra.setColor(ativo ? corAtivo : corInativo);
            barra.setBackground(bgBarra);
            LinearLayout.LayoutParams bLP = new LinearLayout.LayoutParams(dpToPx(18), altBarraPx);
            barra.setLayoutParams(bLP);
            areaBarras.addView(barra);
            col.addView(areaBarras);

            // Label do mês
            TextView tvMes = new TextView(requireContext());
            tvMes.setText(mesesNomes[i]);
            tvMes.setTextSize(10f);
            tvMes.setTextColor(ativo ? corTexto : corTextoFraco);
            if (ativo) tvMes.setTypeface(null, Typeface.BOLD);
            tvMes.setGravity(Gravity.CENTER_HORIZONTAL);
            tvMes.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(20)));
            col.addView(tvMes);

            barChartContainer.addView(col);
        }
    }

    private String[] calcularNomesMeses(int mesAtual) {
        String[] todos = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};
        String[] result = new String[6];
        for (int i = 0; i < 6; i++) {
            result[i] = todos[(mesAtual - 5 + i + 12) % 12];
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    //  Próximas Trocas
    // ─────────────────────────────────────────────────────────────────

    private void renderizarProximasTrocas() {
        proximasTrocasContainer.removeAllViews();

        // Mapa de veículos para lookup por ID
        Map<String, Veiculo> veiculoMap = new java.util.HashMap<>();
        for (Veiculo v : veiculos) {
            if (v.getId() != null) veiculoMap.put(v.getId(), v);
        }

        // Agrupa manutenções por tipo, pegando a mais recente (maior KM) com proximaRevisaoKm definido
        List<Manutencao> sorted = new ArrayList<>(manutencoes);
        Collections.sort(sorted, (a, b) -> {
            long ka = a.getKmRevisao() != null ? a.getKmRevisao() : 0;
            long kb = b.getKmRevisao() != null ? b.getKmRevisao() : 0;
            return Long.compare(kb, ka);
        });

        Map<String, Manutencao> latestPerTipo = new LinkedHashMap<>();
        for (Manutencao m : sorted) {
            if (m.getTipo() == null || m.getProximaRevisaoKm() == null) continue;
            if (!latestPerTipo.containsKey(m.getTipo())) {
                latestPerTipo.put(m.getTipo(), m);
            }
        }

        // Coleta itens de manutenção específicos
        List<long[]> itens = new ArrayList<>(); // [kmRestante, intervalo, isFromVehicle]
        List<String> itensTipo = new ArrayList<>();

        for (Map.Entry<String, Manutencao> entry : latestPerTipo.entrySet()) {
            Manutencao m = entry.getValue();
            Veiculo v = veiculoMap.get(m.getVeiculoId());
            if (v == null || v.getKmAtual() == null) continue;

            long kmRestante = calcularKmRestante(m, v);
            long kmRef = m.getKmRevisao() != null ? m.getKmRevisao() : 0;
            long intervalo = m.getProximaRevisaoKm() - kmRef;
            if (intervalo <= 0) intervalo = 5000;

            itensTipo.add(entry.getKey());
            itens.add(new long[]{kmRestante, intervalo});
        }

        // Fallback: para cada veículo sem manutenção específica, adiciona "Revisão Geral" baseado em kmTroca
        for (Veiculo v : veiculos) {
            if (v.getKmAtual() == null || v.getKmTroca() == null) continue;
            // Só adiciona se não houver revisão geral já na lista vinda das manutenções
            boolean temRevisaoGeral = itensTipo.contains("Revisão Geral");
            if (!temRevisaoGeral) {
                long interval = v.getIntervaloRevisao() != null ? v.getIntervaloRevisao() : 5000L;
                long proxKm = v.getKmTroca() + interval;
                long kmRestante = proxKm - v.getKmAtual();
                itensTipo.add("Revisão Geral");
                itens.add(new long[]{kmRestante, interval});
            }
        }

        // Ordena por urgência (menor km restante primeiro)
        Integer[] indices = new Integer[itens.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Long.compare(itens.get(a)[0], itens.get(b)[0]));

        for (int idx : indices) {
            long kmRestante = itens.get(idx)[0];
            long intervalo = itens.get(idx)[1];
            String tipo = itensTipo.get(idx);

            float progress = intervalo > 0 ? 1f - ((float) Math.max(0, kmRestante) / intervalo) : 1f;
            progress = Math.max(0f, Math.min(1f, progress));

            adicionarLinhaProximaTroca(tipo, kmRestante, progress);
        }

        tvSemTrocas.setVisibility(proximasTrocasContainer.getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private long calcularKmRestante(Manutencao m, Veiculo v) {
        if (v == null || v.getKmAtual() == null || m.getProximaRevisaoKm() == null) return Long.MAX_VALUE;
        return m.getProximaRevisaoKm() - v.getKmAtual();
    }

    private void adicionarLinhaProximaTroca(String tipo, long kmRestante, float progress) {
        int corVerde = getResources().getColor(R.color.success);
        int corAmarelo = getResources().getColor(R.color.warning);
        int corVermelho = getResources().getColor(R.color.error);
        int corFundo = getResources().getColor(R.color.surface_variant);

        int corBarra;
        if (kmRestante > 2000) corBarra = corVerde;
        else if (kmRestante > 500) corBarra = corAmarelo;
        else corBarra = corVermelho;

        // Container da linha
        LinearLayout linha = new LinearLayout(requireContext());
        linha.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.bottomMargin = dpToPx(14);
        linha.setLayoutParams(llp);

        // Linha de texto: tipo + "em X km"
        LinearLayout linhaTexto = new LinearLayout(requireContext());
        linhaTexto.setOrientation(LinearLayout.HORIZONTAL);
        linhaTexto.setGravity(Gravity.CENTER_VERTICAL);
        linhaTexto.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvTipo = new TextView(requireContext());
        tvTipo.setText(tipo);
        tvTipo.setTextSize(13f);
        tvTipo.setTextColor(getResources().getColor(R.color.text_primary));
        tvTipo.setTypeface(null, Typeface.BOLD);
        tvTipo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvKm = new TextView(requireContext());
        String label = kmRestante <= 0
                ? "vencida"
                : "em " + formatarKm(kmRestante) + " km";
        tvKm.setText(label);
        tvKm.setTextSize(12f);
        tvKm.setTextColor(corBarra);
        tvKm.setTypeface(null, Typeface.BOLD);
        tvKm.setGravity(Gravity.END);

        linhaTexto.addView(tvTipo);
        linhaTexto.addView(tvKm);

        // Barra de progresso
        FrameLayout trackContainer = new FrameLayout(requireContext());
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setShape(GradientDrawable.RECTANGLE);
        trackBg.setCornerRadius(dpToPx(3));
        trackBg.setColor(corFundo);
        trackContainer.setBackground(trackBg);
        trackContainer.setClipToOutline(true);
        LinearLayout.LayoutParams trackLP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(6));
        trackLP.topMargin = dpToPx(7);
        trackContainer.setLayoutParams(trackLP);

        // Fill usando LinearLayout com pesos para responsividade
        LinearLayout fillLayout = new LinearLayout(requireContext());
        fillLayout.setOrientation(LinearLayout.HORIZONTAL);
        fillLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (progress > 0) {
            View fill = new View(requireContext());
            fill.setBackgroundColor(corBarra);
            fillLayout.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, progress));
        }
        if (progress < 1f) {
            View empty = new View(requireContext());
            fillLayout.addView(empty, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - progress));
        }

        trackContainer.addView(fillLayout);

        linha.addView(linhaTexto);
        linha.addView(trackContainer);
        proximasTrocasContainer.addView(linha);
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }

    private String formatarMoeda(float valor) {
        if (valor >= 1000) {
            return String.format(Locale.getDefault(), "R$ %.0f", valor)
                    .replace(".", "."); // mantém ponto de milhar quando presente
        }
        return String.format(Locale.getDefault(), "R$ %.0f", valor);
    }

    private String formatarValorCurto(float valor) {
        if (valor >= 1000) {
            return String.format(Locale.getDefault(), "%.0f", valor);
        }
        return String.format(Locale.getDefault(), "%.0f", valor);
    }

    private String formatarKm(long km) {
        if (km >= 1000) {
            return String.format(Locale.getDefault(), "%,.0f", (double) km)
                    .replace(",", ".");
        }
        return String.valueOf(km);
    }
}
