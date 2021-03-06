package org.ohdsi.webapi.cohortresults.mapper;

import org.ohdsi.webapi.cohortresults.CohortAttribute;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CohortAttributeMapper implements RowMapper<CohortAttribute> {

	@Override
	public CohortAttribute mapRow(ResultSet rs, int rowNum) throws SQLException {
		CohortAttribute attribute = new CohortAttribute();
		attribute.setAttributeName(rs.getString("ATTRIBUTE_NAME"));
		attribute.setAttributeValue(rs.getString("ATTRIBUTE_VALUE"));
		return attribute;
	}

}
