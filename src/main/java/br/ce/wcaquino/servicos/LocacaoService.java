package br.ce.wcaquino.servicos;

import static br.ce.wcaquino.utils.DataUtils.adicionarDias;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.ce.wcaquino.dao.LocacaoDao;
import br.ce.wcaquino.entidades.Filme;
import br.ce.wcaquino.entidades.Locacao;
import br.ce.wcaquino.entidades.Usuario;
import br.ce.wcaquino.exceptions.FilmeSemEstoqueException;
import br.ce.wcaquino.exceptions.LocadoraException;
import br.ce.wcaquino.utils.DataUtils;

public class LocacaoService {

	private LocacaoDao dao;

	private SPCService spcService;

	private EmailService emailService;

	public void prorrogarLocacao(Locacao locacao, int dias) {
		Locacao locacao2 = new Locacao();
		locacao2.setUsuario(locacao.getUsuario());
		locacao2.setFilmes(locacao.getFilmes());
		locacao2.setDataLocacao(obterData());
		locacao2.setDataRetorno(DataUtils.obterDataComDiferencaDias(dias));
		locacao2.setValor(locacao.getValor() * dias);
		dao.salvar(locacao2);
	}

	public Locacao alugarFilme(Usuario usuario, List<Filme> filmes) throws FilmeSemEstoqueException, LocadoraException {

		if (usuario == null) {
			throw new LocadoraException("Usuário vazio");
		}

		if (filmes == null || filmes.isEmpty()) {
			throw new LocadoraException("Filme vazio");
		}

		for (Filme filme : filmes) {
			if (filme.getEstoque() == 0) {
				throw new FilmeSemEstoqueException();
			}
		}

		boolean negativado;
		try {
			negativado = spcService.possuiNegativacao(usuario);
		} catch (Exception e) {
			throw new LocadoraException("Problemas com SPC, tente novamente!");
		}

		if (negativado) {
			throw new LocadoraException("Usuário negativo");
		}

		Locacao locacao = new Locacao();
		locacao.setFilmes(filmes);
		locacao.setUsuario(usuario);
		locacao.setDataLocacao(obterData());
		locacao.setValor(calcularValorLocacao(filmes));

		// Entrega no dia seguinte
		Date dataEntrega = obterData();
		dataEntrega = adicionarDias(dataEntrega, 1);
		if (DataUtils.verificarDiaSemana(dataEntrega, Calendar.SUNDAY)) {
			dataEntrega = adicionarDias(dataEntrega, 1);
		}
		locacao.setDataRetorno(dataEntrega);

		// Salvando a locacao...
		// TODO adicionar método para salvar
		dao.salvar(locacao);

		return locacao;

	}

	protected Date obterData() {
		return new Date();
	}

	private Double calcularValorLocacao(List<Filme> filmes) {
		System.out.println("Entrou aqui, estou calculando");
		Double valorTotal = 0d;
		for (int i = 0; i < filmes.size(); i++) {
			Filme filme = filmes.get(i);
			Double valorFilme = filme.getPrecoLocacao();
			switch (i) {
			case 2:
				valorFilme = valorFilme * 0.75;
				break;
			case 3:
				valorFilme = valorFilme * 0.50;
				break;
			case 4:
				valorFilme = valorFilme * 0.25;
				break;
			case 5:
				valorFilme = valorFilme * 0.00;
				break;
			}
			valorTotal += valorFilme;

		}
		return valorTotal;
	}

	public void notificarAtrasos() {
		List<Locacao> locacoes = dao.obterLocacoesPendentes();
		for (Locacao locacao : locacoes) {
			if (locacao.getDataRetorno().before(obterData()))
				emailService.notificarAtraso(locacao.getUsuario());
		}
	}

//	public void setLocacaoDao(LocacaoDao dao) {
//		this.dao = dao;
//	}
//
//	public void setSPCService(SPCService sprcService) {
//		this.spcService = sprcService;
//	}
//
//	public void setEmailService(EmailService emailService) {
//		this.emailService = emailService;
//	}

}