package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QuerySnapshot;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CadastroManutencaoActivity;
import br.jss.motoreviso.activities.DetalheManutencaoActivity;
import br.jss.motoreviso.adapters.ManutencaoAdapterAvancado;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PainelManutencoesFragment extends Fragment {
    private static final String TAG = "PainelManutencoesFragment";

    private RecyclerView recyclerView;
    private ManutencaoAdapterAvancado adapter;
    private List<Manutencao> todasManutemcoes;
    private List<Manutencao> manutencoesFiltrads;
    private Button btnAdicionarManutencao;
    private ProgressBar progressBar;
    private TextView textVazioMensagem;
    private TextView textCustoTotal;
    private TextView textQuantidade;
    private TextView textUltimaManutencao;
    private Spinner spinnerFiltro;
    private Spinner spinnerOrdenacao;
    private FirebaseManager firebaseManager;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_painel_manutencoes, container, false);

        recyclerView = view.findViewById(R.id.recycler_manutencoes);
        btnAdicionarManutencao = view.findViewById(R.id.btn_adicionar_manutencao);
        progressBar = view.findViewById(R.id.progress_bar);
        textVazioMensagem = view.findViewById(R.id.text_vazio);
        textCustoTotal = view.findViewById(R.id.text_custo_total);
        textQuantidade = view.findViewById(R.id.text_quantidade);
        textUltimaManutencao = view.findViewById(R.id.text_ultima_manutencao);
        spinnerFiltro = view.findViewById(R.id.spinner_filtro);
        spinnerOrdenacao = view.findViewById(R.id.spinner_ordenacao);

        firebaseManager = FirebaseManager.getInstance();
        todasManutemcoes = new ArrayList<>();
        manutencoesFiltrads = new ArrayList<>();

        setupRecyclerView();
        setupSpinners();
        carregarManutemcoes();

        btnAdicionarManutencao.setOnClickListener(v -> abrirCadastroManutencao());

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ManutencaoAdapterAvancado(manutencoesFiltrads,
                new ManutencaoAdapterAvancado.OnManutencaoClickListener() {
                    @Override
                    public void onManutencaoClick(Manutencao manutencao) {
                        abrirDetalheManutencao(manutencao);
                    }

                    @Override
                    public void onEditClick(Manutencao manutencao) {
                        // Implementação futura
                        Log.d(TAG, "Editar: " + manutencao.getId());
                    }

                    @Override
                    public void onDeleteClick(Manutencao manutencao) {
                        confirmarDeletacao(manutencao);
                    }
                });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> adapterFiltro = ArrayAdapter.createFromResource(
                getContext(),
                R.array.filtro_manutencao,
                android.R.layout.simple_spinner_item
        );
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterFiltro);
        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltros();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<CharSequence> adapterOrdenacao = ArrayAdapter.createFromResource(
                getContext(),
                R.array.ordenacao_manutencao,
                android.R.layout.simple_spinner_item
        );
        adapterOrdenacao.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrdenacao.setAdapter(adapterOrdenacao);
        spinnerOrdenacao.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarOrdenacao();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void carregarManutemcoes() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterTodasManutemcoes().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful()) {
                todasManutemcoes.clear();
                QuerySnapshot snapshot = task.getResult();

                if (snapshot != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Manutencao manutencao = doc.toObject(Manutencao.class);
                        if (manutencao != null) {
                            manutencao.setId(doc.getId());
                            todasManutemcoes.add(manutencao);
                        }
                    }
                }

                Collections.sort(todasManutemcoes, (a, b) -> {
                    Long dataA = a.getDataRevisao() != null ? a.getDataRevisao() : 0L;
                    Long dataB = b.getDataRevisao() != null ? b.getDataRevisao() : 0L;
                    return Long.compare(dataB, dataA);
                });

                aplicarFiltros();
                atualizarEstatisticas();

            } else {
                Log.e(TAG, "Erro ao carregar manutenções", task.getException());
                textVazioMensagem.setText("Erro ao carregar manutenções");
                textVazioMensagem.setVisibility(View.VISIBLE);
            }
        });
    }

    private void aplicarFiltros() {
        String filtroSelecionado = spinnerFiltro.getSelectedItem().toString();
        manutencoesFiltrads.clear();

        for (Manutencao m : todasManutemcoes) {
            if (filtroSelecionado.equals("Todas")) {
                manutencoesFiltrads.add(m);
            } else if (m.getTipo() != null && filtroSelecionado.equals(m.getTipo())) {
                manutencoesFiltrads.add(m);
            }
        }

        aplicarOrdenacao();
    }

    private void aplicarOrdenacao() {
        String ordenacao = spinnerOrdenacao.getSelectedItem().toString();

        if (ordenacao.equals("Mais recente")) {
            Collections.sort(manutencoesFiltrads, (a, b) -> {
                Long dataA = a.getDataRevisao() != null ? a.getDataRevisao() : 0L;
                Long dataB = b.getDataRevisao() != null ? b.getDataRevisao() : 0L;
                return Long.compare(dataB, dataA);
            });
        } else if (ordenacao.equals("Maior custo")) {
            Collections.sort(manutencoesFiltrads, (a, b) -> {
                Double custoA = a.getCusto() != null ? a.getCusto() : 0.0;
                Double custoB = b.getCusto() != null ? b.getCusto() : 0.0;
                return Double.compare(custoB, custoA);
            });
        } else if (ordenacao.equals("Maior KM")) {
            Collections.sort(manutencoesFiltrads, (a, b) -> {
                Long kmA = a.getKmRevisao() != null ? a.getKmRevisao() : 0L;
                Long kmB = b.getKmRevisao() != null ? b.getKmRevisao() : 0L;
                return Long.compare(kmB, kmA);
            });
        }

        adapter.notifyDataSetChanged();
        atualizarVisibilidade();
    }

    private void atualizarEstatisticas() {
        double custoTotal = 0;

        for (Manutencao m : todasManutemcoes) {
            if (m.getCusto() != null) {
                custoTotal += m.getCusto();
            }
        }

        textCustoTotal.setText(String.format("Total: R$ %.2f", custoTotal));
        textQuantidade.setText(String.format("Total de manutenções: %d", todasManutemcoes.size()));

        if (!todasManutemcoes.isEmpty()) {
            Manutencao ultima = todasManutemcoes.get(0);
            if (ultima.getDataRevisao() != null) {
                String dataUltima = sdf.format(ultima.getDataRevisao());
                textUltimaManutencao.setText(String.format("Última: %s", dataUltima));
            } else {
                textUltimaManutencao.setText("Última: -");
            }
        } else {
            textUltimaManutencao.setText("Última: -");
        }
    }

    private void atualizarVisibilidade() {
        if (manutencoesFiltrads.isEmpty()) {
            textVazioMensagem.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textVazioMensagem.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void abrirDetalheManutencao(Manutencao manutencao) {
        Intent intent = new Intent(getActivity(), DetalheManutencaoActivity.class);
        intent.putExtra("MANUTENCAO_ID", manutencao.getId());
        startActivity(intent);
    }

    private void abrirCadastroManutencao() {
        Intent intent = new Intent(getActivity(), CadastroManutencaoActivity.class);
        startActivity(intent);
    }

    private void confirmarDeletacao(Manutencao manutencao) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirmar exclusão")
                .setMessage("Deseja deletar esta manutenção?")
                .setPositiveButton("Sim", (dialog, which) -> deletarManutencao(manutencao))
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deletarManutencao(Manutencao manutencao) {
        firebaseManager.deletarManutencao(manutencao.getId()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                todasManutemcoes.remove(manutencao);
                aplicarFiltros();
                atualizarEstatisticas();
            } else {
                Log.e(TAG, "Erro ao deletar", task.getException());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarManutemcoes();
    }
}