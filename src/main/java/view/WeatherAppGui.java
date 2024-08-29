package view;

import model.Cidade;
import model.ListaCidades;
import model.Previsao;
import model.PrevisaoCidade;
import model.parse.XStreamParser;
import model.service.WeatherForecastService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WeatherAppGui extends JFrame {
    private XStreamParser<PrevisaoCidade, ListaCidades> parserCidades;
    private XStreamParser<PrevisaoCidade, ListaCidades> parserPrevisoes;
    private JTable tabelaCidades;
    private DefaultTableModel modeloTabela;
    private JLabel rotuloStatus;
    private JLabel rotuloCarregando;

    // Mapeia as siglas para suas descrições
    private static final Map<String, String> condicoesTempo;

    static {
        condicoesTempo = new HashMap<>();
        condicoesTempo.put("ec", "Encoberto com Chuvas Isoladas");
        condicoesTempo.put("ci", "Chuvas Isoladas");
        condicoesTempo.put("c", "Chuva");
        condicoesTempo.put("in", "Instável");
        condicoesTempo.put("pp", "Poss. de Pancadas de Chuva");
        condicoesTempo.put("cm", "Chuva pela Manhã");
        condicoesTempo.put("cn", "Chuva a Noite");
        condicoesTempo.put("pt", "Pancadas de Chuva a Tarde");
        condicoesTempo.put("pm", "Pancadas de Chuva pela Manhã");
        condicoesTempo.put("np", "Nublado e Pancadas de Chuva");
        condicoesTempo.put("pc", "Pancadas de Chuva");
        condicoesTempo.put("pn", "Parcialmente Nublado");
        condicoesTempo.put("cv", "Chuvisco");
        condicoesTempo.put("ch", "Chuvoso");
        condicoesTempo.put("t", "Tempestade");
        condicoesTempo.put("ps", "Predomínio de Sol");
        condicoesTempo.put("e", "Encoberto");
        condicoesTempo.put("n", "Nublado");
        condicoesTempo.put("cl", "Céu Claro");
        condicoesTempo.put("nv", "Nevoeiro");
        condicoesTempo.put("g", "Geada");
        condicoesTempo.put("ne", "Neve");
        condicoesTempo.put("nd", "Não Definido");
        condicoesTempo.put("pnt", "Pancadas de Chuva a Noite");
        condicoesTempo.put("psc", "Possibilidade de Chuva");
        condicoesTempo.put("pcm", "Possibilidade de Chuva pela Manhã");
        condicoesTempo.put("pct", "Possibilidade de Chuva a Tarde");
        condicoesTempo.put("pcn", "Possibilidade de Chuva a Noite");
        condicoesTempo.put("npt", "Nublado com Pancadas a Tarde");
        condicoesTempo.put("npn", "Nublado com Pancadas a Noite");
        condicoesTempo.put("ncn", "Nublado com Poss. de Chuva a Noite");
        condicoesTempo.put("nct", "Nublado com Poss. de Chuva a Tarde");
        condicoesTempo.put("ncm", "Nubl. c/ Poss. de Chuva pela Manhã");
        condicoesTempo.put("npm", "Nublado com Pancadas pela Manhã");
        condicoesTempo.put("npp", "Nublado com Possibilidade de Chuva");
        condicoesTempo.put("vn", "Variação de Nebulosidade");
        condicoesTempo.put("ct", "Chuva a Tarde");
        condicoesTempo.put("ppn", "Poss. de Panc. de Chuva a Noite");
        condicoesTempo.put("ppt", "Poss. de Panc. de Chuva a Tarde");
        condicoesTempo.put("ppm", "Poss. de Panc. de Chuva pela Manhã");
    }

    public WeatherAppGui() {
        super("Aplicativo do Tempo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(true);

        parserCidades = new XStreamParser<>();
        parserPrevisoes = new XStreamParser<>();
        adicionarComponentesGui();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherAppGui().setVisible(true));
    }

    private void adicionarComponentesGui() {
        JPanel painelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JLabel rotuloCidade = new JLabel("Cidade:");
        JTextField campoPesquisa = new JTextField(20);
        JButton botaoPesquisar = new JButton("Pesquisar");

        painelSuperior.add(rotuloCidade);
        painelSuperior.add(campoPesquisa);
        painelSuperior.add(botaoPesquisar);

        add(painelSuperior, BorderLayout.NORTH);

        JPanel painelStatus = new JPanel(new BorderLayout());
        rotuloStatus = new JLabel(" ");
        painelStatus.add(rotuloStatus, BorderLayout.CENTER);
        add(painelStatus, BorderLayout.SOUTH);

        JPanel painelCarregamento = new JPanel(new BorderLayout());
        rotuloCarregando = new JLabel("Carregando...");
        rotuloCarregando.setVisible(false);
        painelCarregamento.add(rotuloCarregando, BorderLayout.CENTER);
        add(painelCarregamento, BorderLayout.SOUTH);

        String[] nomesColunas = {"Nome da Cidade", "Estado", "Data", "Temperatura (Max / Min)", "Condição"};
        modeloTabela = new DefaultTableModel(nomesColunas, 0);
        tabelaCidades = new JTable(modeloTabela);
        tabelaCidades.setCellSelectionEnabled(false);
        tabelaCidades.setDefaultEditor(Object.class, null);
        JScrollPane painelRolagem = new JScrollPane(tabelaCidades);

        add(painelRolagem, BorderLayout.CENTER);

        botaoPesquisar.addActionListener(e -> realizarBusca(campoPesquisa));

        campoPesquisa.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    realizarBusca(campoPesquisa);
                }
            }
        });
    }

    private void realizarBusca(JTextField campoPesquisa) {
        String nomeCidade = formatarTexto(campoPesquisa.getText().trim());
        if (nomeCidade.isEmpty()) {
            mostrarErro("Por favor, insira um nome de cidade.");
            return;
        }

        // Limpa a tabela e o campo de status
        modeloTabela.setRowCount(0);
        rotuloStatus.setText("Buscando cidades...");
        rotuloCarregando.setVisible(true);
        campoPesquisa.setText(""); // Limpa o campo de pesquisa

        // Verifica se a tabela está vazia antes de mostrar o carregamento
        boolean tabelaVazia = modeloTabela.getRowCount() == 0;

        SwingUtilities.invokeLater(() -> {
            try {
                String cidadesXML = WeatherForecastService.cidades(nomeCidade);

                if (cidadesXML.trim().equals("<cidades/>")) {
                    mostrarErro("Nenhuma cidade encontrada com o nome informado.");
                    rotuloCarregando.setVisible(false);
                    return;
                }

                ListaCidades listaCidades = parserCidades.cidades(cidadesXML);

                if (listaCidades == null || listaCidades.getCidades() == null || listaCidades.getCidades().isEmpty()) {
                    mostrarErro("Nenhuma cidade encontrada com o nome informado.");
                    rotuloCarregando.setVisible(false);
                    return;
                }

                SimpleDateFormat formatoEntrada = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat formatoSaida = new SimpleDateFormat("dd/MM/yyyy");

                for (Cidade cidade : listaCidades.getCidades()) {
                    int codCidade = cidade.getId();
                    String nomeCidadeLocal = cidade.getNome();
                    String uf = cidade.getUf();

                    try {
                        String previsaoXML = WeatherForecastService.previsoesParaSeteDias(codCidade);

                        if (previsaoXML.trim().equals("<previsoes/>")) {
                            modeloTabela.addRow(new Object[]{nomeCidadeLocal, uf, "Sem Dados", "Sem Dados", "Sem Dados"});
                            continue;
                        }

                        PrevisaoCidade previsaoCidade = parserPrevisoes.previsao(previsaoXML);

                        if (previsaoCidade != null && previsaoCidade.getPrevisoes() != null && !previsaoCidade.getPrevisoes().isEmpty()) {
                            for (Previsao p : previsaoCidade.getPrevisoes()) {
                                String maxima = p.getMaxima();
                                String minima = p.getMinima();
                                String temperatura = maxima + " / " + minima;
                                String tempo = condicoesTempo.getOrDefault(p.getTempo(), "Descrição Não Disponível");

                                // Formatar a data
                                Date data = formatoEntrada.parse(p.getDia());
                                String dataFormatada = formatoSaida.format(data);

                                modeloTabela.addRow(new Object[]{nomeCidadeLocal, uf, dataFormatada, temperatura, tempo});
                            }
                        } else {
                            modeloTabela.addRow(new Object[]{nomeCidadeLocal, uf, "Sem Dados", "Sem Dados", "Sem Dados"});
                        }
                    } catch (ParseException ex) {
                        mostrarErro("Erro ao formatar a data: " + ex.getMessage());
                    }
                }

                rotuloStatus.setText("Busca concluída.");
            } catch (IOException ex) {
                mostrarErro("Erro ao buscar dados: " + ex.getMessage());
            } finally {
                // Só oculta o painel de carregamento se a tabela estiver vazia
                if (tabelaVazia) {
                    rotuloCarregando.setVisible(false);
                }
            }
        });
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private String formatarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        // Troca espaços por _ e remove acentos
        texto = texto.replace(" ", "_");
        texto = Normalizer.normalize(texto, Form.NFD);
        texto = texto.replaceAll("\\p{M}", "");
        return texto.toLowerCase();
    }
}
