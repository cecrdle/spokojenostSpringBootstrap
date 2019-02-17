package cz.hb.app.generatecsv.spokojenost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExportToCSV implements CommandLineRunner {
	@Autowired
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Override
	public void run(String... args) throws Exception {
		Date datumFrom = null;
		Date datumTo = null;
		if (args.length == 2) {
			try {
				datumFrom = new SimpleDateFormat("dd.MM.yyyy").parse(args[0]);
				datumTo = new SimpleDateFormat("dd.MM.yyyy").parse(args[0]);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			datumTo=new java.util.Date();
			Calendar cal=Calendar.getInstance();
			cal.setTime(datumTo);
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.MILLISECOND,0);	
			datumTo=cal.getTime();
			cal.add(Calendar.DAY_OF_YEAR,-7);
			datumFrom=cal.getTime();
			System.out.println(datumTo);
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(datumTo);
		cal.set(Calendar.HOUR, 23);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.MILLISECOND,59);
		datumTo = cal.getTime();
		SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy");
		System.out.println("Running export od "+sdf.format(datumFrom)+" do "+sdf.format(datumTo));
		CsvWriter output = new CsvWriter(new FileOutputStream(new File("hodnoceni.csv")),
				Charset.forName("Cp1250").newEncoder());
		output.appendValue("Č. úvěru");
		output.appendValue("Jméno osoby");
		output.appendValue("RČ");
		output.appendValue("Přislušnost úvěru");
		output.appendValue("Název dokumentu/události");
		output.appendValue("Typ");
		output.appendValue("Index");
		output.appendValue("Datum vzniku dokumentu/události");
		// output.appendValue("Pracovník");
		output.appendValue("Udělené hodnocení");
		output.appendValue("Komentář k hodnocení");
		output.appendLastValue("Datum a čas hodnocení");
		ResourceBundleMessageSource resource = new ResourceBundleMessageSource();
		resource.setBasename("cz.hb.app.generatecsv.spokojenost.KlientskaZonaDomainResources");
		StringBuilder sql = new StringBuilder();
		sql.append(
				"select u.cislo_uv_kratke, CONCAT(uz.jmeno,' ',uz.prijmeni) as osoba, o.rc, u.oddeleni,ud.typ_udalosti, 'UDALOST' as typ, ud.datum_vzniku, CONCAT(uz2.jmeno,' ',uz2.prijmeni) as pracovnik, uh.hodnoceni, uh.slovni_hodnoceni, uh.datum_hodnoceni,null as index_typ ");
		sql.append(
				" from udalost_hodnoceni uh left join udalost ud on (ud.id_udalost = uh.id_udalost) left join uver u on (u.id_srv = ud.id_srv) left join uzivatel uz on (uz.id_uzivatel = uh.id_uzivatel)  left join uzivatel uz2 on (uz2.id_uzivatel = ud.id_uzivatel) left join kz_uzivatel kz_u on (kz_u.id_uzivatel = uz.id_uzivatel) left join osoba o on (kz_u.id_osoba = o.id_osoba) ");
		sql.append(" where cast(uh.datum_hodnoceni as date) BETWEEN CAST( :datumOd AS DATE ) AND CAST( :datumDo AS DATE )");
		sql.append(" union ");
		sql.append(
				"select u.cislo_uv_kratke, CONCAT(uz.jmeno,' ',uz.prijmeni) as jmeno_osoba,  o.rc, u.oddeleni, d.typ_edm_dokumentu, 'DOKUMENT' as typ, dv.datum_verze, CASE WHEN (uz2.id_uzivatel IS NULL) THEN 'SYSTEM' ELSE  CONCAT(uz2.jmeno,' ',uz2.prijmeni)  END as pracovnik, duh.hodnoceni, duh.slovni_hodnoceni, duh.datum_hodnoceni,d.typ_edm_dokumentu as index_typ ");
		sql.append(
				" from dokument_verze_hodnoceni duh left join dokument_verze dv on (dv.id_dokument_verze = duh.id_dokument_verze) left join dokument d on (d.id_dokument = dv.id_dokument) left join dokument_vyskyt dvy on (dvy.id_dokument = d.id_dokument) left join uver u on (u.id_srv = dvy.id_srv) left join uzivatel uz on (uz.id_uzivatel = duh.id_uzivatel)  left join uzivatel uz2 on (uz.id_uzivatel = d.id_uzivatel_zalozil)  left join kz_uzivatel kz_u on (kz_u.id_uzivatel = uz.id_uzivatel) left join osoba o on (kz_u.id_osoba = o.id_osoba) ");
		sql.append(" where cast(duh.datum_hodnoceni as date) BETWEEN CAST( :datumOd AS DATE ) AND CAST( :datumDo AS DATE )");
		sql.append(" order by datum_hodnoceni ASC");
		//System.out.println(sql.toString());
		MapSqlParameterSource params = new MapSqlParameterSource("datumOd", datumFrom).addValue("datumDo", datumTo);
		namedParameterJdbcTemplate.query(sql.toString(), params, (ResultSet rs) -> {
			do {
				try {
					output.appendValue(rs.getString("cislo_uv_kratke"));
					output.appendValue(rs.getString("osoba"));
					output.appendValue(rs.getString("rc"));
					output.appendValue(rs.getString("oddeleni"));
					String typ = rs.getString("typ");
					String typUdalost = rs.getString("typ_udalosti");
					String typIndex = rs.getString("index_typ");
					String popis = null;
					String index = null;
					if (typ.equals("UDALOST")) {
						popis = resource.getMessage(
								"cz.hb.portal.remote.klientskazona.domain.udalost.types.TypUdalosti." + typUdalost,
								null, typUdalost, new Locale("cs", "CZ"));
						if (typUdalost.startsWith("DOKUMENT_")) {
							index = typUdalost.substring("DOKUMENT_".length() - 1);
						}
					} else {
						popis = resource.getMessage(
								"cz.hb.portal.remote.klientskazona.domain.dokument.types.TypDokumentuEDM." + typUdalost,
								null, typUdalost, new Locale("cs", "CZ"));
						index = typIndex;
					}
					output.appendValue(popis);
					output.appendValue(rs.getString("typ"));
					output.appendValue(index);
					output.appendValue(
							new SimpleDateFormat("dd.MM.yyyy HH:mm").format(rs.getTimestamp("datum_vzniku")));
					// output.appendValue(rs.getString("pracovnik"));
					output.appendValue(rs.getString("hodnoceni"));
					output.appendValue(rs.getString("slovni_hodnoceni") == null ? ""
							: "\"" + rs.getString("slovni_hodnoceni") + "\"");
					output.appendLastValue(
							new SimpleDateFormat("dd.MM.yyyy HH:mm").format(rs.getTimestamp("datum_hodnoceni")));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} while (rs.next());
		});
		output.close();
		System.out.println("Running export done");
	}

}
