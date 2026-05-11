package autodromo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import com.sun.jna.platform.win32.User32;  // bloquear print
import java.awt.datatransfer.StringSelection; // bloquear print


public class CronometroAutodromo extends WindowAdapter implements ActionListener {
    private JFrame janela;
    private JTabbedPane abas;
    private JPanel painelCadastro, painelResultados, painelTabela;
    private Label lCronometro;
    
    // Campos
    private TextField tNomeCorredor, tEquipe;
    private TextField resCorredor, resEquipe, resVolta1, resVolta2, resTotal;
    private Button bAcao, bNovo, bBuscar, bExcluir, bSalvarEdicao, bAtualizarTabela;
    private java.util.List<String[]> listaResultados = new java.util.ArrayList<>();
    private int indiceAtual = -1;
    private Button bAnterior, bProximo;
    private boolean altTabAtivo = false;
    
    // Tabela
    private JTable tabelaRank;
    private DefaultTableModel modeloTabela;

    private Timer timer;
    private long tempoInicial;
    private double tempoVolta1, tempoVolta2;
    private int estado = 0; 

    private final String URL = "jdbc:mysql://localhost:3306/autodromo";
    private final String USER = "root"; 
    private final String PASS = "";     

    
    public CronometroAutodromo() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null, "Driver MySQL não encontrado!");
        }

        janela = new JFrame("Autódromo Goiânia - Gestão de Corridas");
        janela.setSize(500, 650);
        janela.setAlwaysOnTop(true);
        janela.setResizable(false);
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.addWindowFocusListener(new WindowAdapter() {
            
            
    @Override
    public void windowLostFocus(WindowEvent e) {
        // Verifica se a janela que ganhou o foco é do próprio codigo
        Window opposite = e.getOppositeWindow();
        
        if (opposite != null && (opposite instanceof JDialog || opposite instanceof JFrame)) {
            // Se o foco foi para um aviso do próprio Java, não faz nada
            return; 
        }

        // Se o foco foi de fora executa o bloqueio
        janela.setVisible(false);
        JOptionPane.showMessageDialog(null, "ALT + TAB BLOQUEADO!", "Segurança", JOptionPane.WARNING_MESSAGE);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {}

        janela.setVisible(true);
        janela.toFront();
        janela.requestFocus();
    }
});
        
        
        janela.setLayout(null);

        lCronometro = new Label("00:00.000", Label.CENTER);
        lCronometro.setFont(new Font("Arial", Font.BOLD, 45));
        lCronometro.setForeground(new Color(204, 0, 0));
        lCronometro.setBounds(20, 20, 460, 60);
        janela.add(lCronometro);
        
        abas = new JTabbedPane();

        // --- ABA 1: CADASTRO ---
        painelCadastro = new JPanel(null);
        int yC = 30;
        painelCadastro.add(new JLabel("PILOTO:")).setBounds(20, yC, 100, 20);
        tNomeCorredor = new TextField(); tNomeCorredor.setBounds(130, yC, 250, 20);
        painelCadastro.add(tNomeCorredor);
        painelCadastro.add(new JLabel("EQUIPE:")).setBounds(20, yC += 40, 100, 20);
        tEquipe = new TextField(); tEquipe.setBounds(130, yC, 250, 20);
        painelCadastro.add(tEquipe);

        // --- ABA 2: RESULTADOS ---
        painelResultados = new JPanel(null);
        int yR = 20;
        painelResultados.add(new JLabel("CORREDOR:")).setBounds(20, yR, 80, 20);
        resCorredor = new TextField(); resCorredor.setBounds(110, yR, 200, 20);
        painelResultados.add(resCorredor);
        
        bBuscar = new Button("Buscar Equipe");
        bBuscar.setBounds(320, yR, 120, 22);
        bBuscar.addActionListener(this);
        painelResultados.add(bBuscar);

        painelResultados.add(new JLabel("EQUIPE:")).setBounds(20, yR += 30, 80, 20);
        resEquipe = new TextField(); resEquipe.setBounds(110, yR, 200, 20);
        painelResultados.add(resEquipe);
        
        painelResultados.add(new JLabel("VOLTA 1:")).setBounds(20, yR += 40, 80, 20);
        resVolta1 = new TextField(); resVolta1.setBounds(110, yR, 100, 20);
        painelResultados.add(resVolta1);
        
        painelResultados.add(new JLabel("VOLTA 2:")).setBounds(20, yR += 30, 80, 20);
        resVolta2 = new TextField(); resVolta2.setBounds(110, yR, 100, 20);
        painelResultados.add(resVolta2);
        
        painelResultados.add(new JLabel("TOTAL:")).setBounds(20, yR += 40, 80, 20);
        resTotal = new TextField(); resTotal.setBounds(110, yR, 100, 20);
        resTotal.setBackground(Color.BLACK); resTotal.setForeground(Color.YELLOW);
        painelResultados.add(resTotal);
        
        

        bSalvarEdicao = new Button("Salvar Alterações");
        bSalvarEdicao.setBounds(60, yR += 50, 150, 30);
        bSalvarEdicao.addActionListener(this);
        painelResultados.add(bSalvarEdicao);

        bExcluir = new Button("Excluir Registro");
        bExcluir.setBounds(230, yR, 150, 30);
        bExcluir.setBackground(new Color(255, 100, 100));
        bExcluir.addActionListener(this);
        painelResultados.add(bExcluir);
        
        // Abaixo do bExcluir, adicione:
        bAnterior = new Button("< Anterior");
        bAnterior.setBounds(60, yR += 40, 150, 30);
        bAnterior.addActionListener(this);
        painelResultados.add(bAnterior);

        bProximo = new Button("Próximo >");
        bProximo.setBounds(230, yR, 150, 30);
        bProximo.addActionListener(this);
        painelResultados.add(bProximo);

        // --- ABA 3: TABELA DE RANKING ---
        painelTabela = new JPanel(new BorderLayout());
        modeloTabela = new DefaultTableModel(new Object[]{"Pos", "Equipe", "Corredor", "Tempo Total"}, 0);
        tabelaRank = new JTable(modeloTabela);
        JScrollPane scroll = new JScrollPane(tabelaRank);
        
        bAtualizarTabela = new Button("Atualizar Ranking");
        bAtualizarTabela.addActionListener(this);
        
        painelTabela.add(scroll, BorderLayout.CENTER);
        painelTabela.add(bAtualizarTabela, BorderLayout.SOUTH);

        abas.addTab("Cadastro", painelCadastro);
        abas.addTab("Resultados", painelResultados);
        abas.addTab("Tabela Ranking", painelTabela);
        abas.setBounds(20, 100, 460, 320);

        bAcao = new Button("INICIAR CORRIDA");
        bAcao.setBounds(150, 430, 200, 45);
        bAcao.addActionListener(this);
        
        bNovo = new Button("Resetar Tudo");
        bNovo.setBounds(175, 490, 150, 30);
        bNovo.addActionListener(this);

        janela.add(abas); janela.add(bAcao); janela.add(bNovo);

        bloquearPrintScreen();  // pra bloquear o print (no final do codigo)
        
        timer = new Timer(10, e -> {
            long diff = System.currentTimeMillis() - tempoInicial;
            lCronometro.setText(formatarTempo(diff));
        });
        
        atualizarTabela(); // Carrega ao iniciar
    }

    @Override
public void actionPerformed(ActionEvent e) {
    if (e.getSource() == bAcao) logicaCronometro();
    else if (e.getSource() == bNovo) resetar();
    else if (e.getSource() == bBuscar) buscarNoBanco();
    else if (e.getSource() == bSalvarEdicao) salvarEdicao();
    else if (e.getSource() == bExcluir) excluirDoBanco();
    else if (e.getSource() == bAtualizarTabela) atualizarTabela();
    else if (e.getSource() == bAnterior) navegar(-1);
    else if (e.getSource() == bProximo) navegar(1);   
}

    private void atualizarTabela() {
        modeloTabela.setRowCount(0); // Limpa a tabela atual
        //SQL
        String sql = "SELECT equipe, corredor, tempo_total FROM corridas ORDER BY tempo_total ASC";
        
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            int pos = 1;
            while (rs.next()) {
                modeloTabela.addRow(new Object[]{
                    pos++, 
                    rs.getString("equipe"), 
                    rs.getString("corredor"), 
                    rs.getString("tempo_total")
                });
            }
        } catch (Exception ex) {
            System.err.println("Erro ao carregar tabela: " + ex.getMessage());
        }
    }

    private void logicaCronometro() {
        if (estado == 0) {
            tempoInicial = System.currentTimeMillis();
            timer.start();
            estado = 1;
            bAcao.setLabel("REGISTRAR VOLTA 1");
            resCorredor.setText(tNomeCorredor.getText());
            resEquipe.setText(tEquipe.getText());
        } else if (estado == 1) {
            long tempoMs = System.currentTimeMillis() - tempoInicial;
            tempoVolta1 = tempoMs / 1000.0;
            resVolta1.setText(formatarTempo(tempoMs));
            tempoInicial = System.currentTimeMillis(); 
            estado = 2;
            bAcao.setLabel("REGISTRAR VOLTA 2");
        } else if (estado == 2) {
            timer.stop();
            long tempoMs = System.currentTimeMillis() - tempoInicial;
            tempoVolta2 = tempoMs / 1000.0;
            resVolta2.setText(formatarTempo(tempoMs));
            resTotal.setText(String.format("%.3f s", tempoVolta1 + tempoVolta2));
            estado = 3;
            bAcao.setEnabled(false);
            bAcao.setLabel("CORRIDA FINALIZADA");
            inserirNovoNoBanco(); 
            atualizarTabela(); // Atualiza a tabela após salvar
            abas.setSelectedIndex(2); // Muda para a aba da tabela
        }
    }

    private void inserirNovoNoBanco() {
        String sql = "INSERT INTO corridas (corredor, equipe, volta1, volta2, tempo_total) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resCorredor.getText());
            ps.setString(2, resEquipe.getText());
            ps.setString(3, resVolta1.getText());
            ps.setString(4, resVolta2.getText());
            ps.setString(5, resTotal.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(janela, "Corrida Salva!");
        } catch (Exception ex) { JOptionPane.showMessageDialog(janela, "Erro: " + ex.getMessage()); }
    }

    private void buscarNoBanco() {
    String termoEquipe = resEquipe.getText().trim();
    String termoCorredor = resCorredor.getText().trim();
    
    // Busca por inicial ou traz tudo se ambos estiverem vazios
    String sql = "SELECT * FROM corridas WHERE equipe LIKE ? AND corredor LIKE ? ORDER BY corredor ASC";
    
    listaResultados.clear(); // Limpa a lista anterior

    try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setString(1, termoEquipe + "%");
        ps.setString(2, termoCorredor + "%");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            listaResultados.add(new String[]{
                rs.getString("corredor"),
                rs.getString("equipe"),
                rs.getString("volta1"),
                rs.getString("volta2"),
                rs.getString("tempo_total")
            });
        }

        if (!listaResultados.isEmpty()) {
            indiceAtual = 0;
            mostrarDadosNavegacao();
        } else {
            JOptionPane.showMessageDialog(janela, "Nenhum registro encontrado.");
        }
    } catch (Exception ex) { ex.printStackTrace(); }
}

private void navegar(int direcao) {
    // Se a lista estiver vazia, tenta carregar todos os pilotos primeiro
    if (listaResultados.isEmpty()) {
        resEquipe.setText("");
        resCorredor.setText("");
        buscarNoBanco();
        return;
    }

    int novoIndice = indiceAtual + direcao;

    // Verifica se está dentro dos limites da lista
    if (novoIndice >= 0 && novoIndice < listaResultados.size()) {
        indiceAtual = novoIndice;
        mostrarDadosNavegacao();
    } else {
        String msg = (direcao > 0) ? "Fim da lista." : "Início da lista.";
        JOptionPane.showMessageDialog(janela, msg);
    }
}

private void mostrarDadosNavegacao() {
    String[] dados = listaResultados.get(indiceAtual);
    resCorredor.setText(dados[0]);
    resEquipe.setText(dados[1]);
    resVolta1.setText(dados[2]);
    resVolta2.setText(dados[3]);
    resTotal.setText(dados[4]);
}

    private void salvarEdicao() {
        String sql = "UPDATE corridas SET corredor=?, volta1=?, volta2=?, tempo_total=? WHERE equipe=?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resCorredor.getText());
            ps.setString(2, resVolta1.getText());
            ps.setString(3, resVolta2.getText());
            ps.setString(4, resTotal.getText());
            ps.setString(5, resEquipe.getText());
            ps.executeUpdate();
            atualizarTabela();
            JOptionPane.showMessageDialog(janela, "Atualizado!");
        } catch (Exception ex) { }
    }

    private void excluirDoBanco() {
        String sql = "DELETE FROM corridas WHERE equipe = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resEquipe.getText());
            ps.executeUpdate();
            atualizarTabela();
            resetar();
        } catch (Exception ex) { }
    }

    private void resetar() {
        timer.stop(); estado = 0;
        lCronometro.setText("00:00.000");
        tNomeCorredor.setText(""); tEquipe.setText("");
        resCorredor.setText(""); resEquipe.setText(""); resVolta1.setText(""); 
        resVolta2.setText(""); resTotal.setText("");
        bAcao.setEnabled(true); bAcao.setLabel("INICIAR CORRIDA");
        abas.setSelectedIndex(0);
    }

    private String formatarTempo(long ms) {
        return String.format("%02d:%02d.%03d", (ms / 60000), (ms % 60000) / 1000, ms % 1000);
    }
private void bloquearPrintScreen() {

    Timer bloqueio = new Timer(50, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {

            try {

                short tecla = User32.INSTANCE.GetAsyncKeyState(0x2C);

                if ((tecla & 0x8000) != 0 && !altTabAtivo) {

                    // Apaga o clipbord
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(new StringSelection(""), null);

                    // Esconde o conteudo
                    altTabAtivo = true;
                    janela.setVisible(false);

                    JOptionPane.showMessageDialog(
                            null,
                            "PRINT SCREEN BLOQUEADO!",
                            "Segurança",
                            JOptionPane.WARNING_MESSAGE
                    );

                    Thread.sleep(1000);

                    janela.setVisible(true);
                }

            } catch (Exception ex) {
                System.out.println("Proteção ativa");
            }
        }
    });

    bloqueio.start();
}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CronometroAutodromo().janela.setVisible(true));
    }
    
}