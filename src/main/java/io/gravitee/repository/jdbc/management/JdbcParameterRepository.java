/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.jdbc.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.jdbc.orm.JdbcObjectMapper;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author njt
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
@Repository
public class JdbcParameterRepository implements ParameterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcParameterRepository.class);
    private static final JdbcObjectMapper ORM =
            JdbcObjectMapper.builder(Parameter.class, "parameters")
                    .updateSql("update parameters set "
                            + escapeReservedWord("key") + " = ?"
                            + " , reference_type = ?"
                            + " , reference_id = ?"
                            + " , value = ?"
                            + " where "
                            + escapeReservedWord("key") + " = ? "
                            + "and reference_type = ? "
                            + "and reference_id = ? "
                    )
                    .addColumn("key", Types.NVARCHAR, String.class)
                    .addColumn("reference_type", Types.NVARCHAR, ParameterReferenceType.class)
                    .addColumn("reference_id", Types.NVARCHAR, String.class)
                    .addColumn("value", Types.NVARCHAR, String.class)
                    .build();
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Parameter create(Parameter parameter) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.create({})", parameter);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(parameter));
            return findById(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create parameter", ex);
            throw new TechnicalException("Failed to create parameter", ex);
        }
    }

    @Override
    public Parameter update(Parameter parameter) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.update({})", parameter);
        if (parameter == null) {
            throw new IllegalStateException("Failed to update null");
        }
        try {
            final PreparedStatementCreator psc = ORM.buildUpdatePreparedStatementCreator(parameter
                    , parameter.getKey()
                    , parameter.getReferenceType().name()
                    , parameter.getReferenceId()
            );
            jdbcTemplate.update(psc);

            return findById(parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType()).orElseThrow(() ->
                    new IllegalStateException(format("No parameter found with id [%s, %s, %s]", parameter.getKey(), parameter.getReferenceId(), parameter.getReferenceType())));
        } catch (final IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update parameter", ex);
            throw new TechnicalException("Failed to update parameter", ex);
        }
    }

    @Override
    public void delete(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.delete({}, {}, {})", key, referenceId, referenceType);
        try {
            jdbcTemplate.update("delete from parameters where " + escapeReservedWord("key") + " = ? and reference_type = ? and reference_id = ? "
                    , key
                    , referenceType.name()
                    , referenceId
            );
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete parameter", ex);
            throw new TechnicalException("Failed to delete parameter", ex);
        }
    }

    @Override
    public Optional<Parameter> findById(String key, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findById({}, {}, {})", key, referenceId, referenceType);
        try {
            final List<Parameter> items = jdbcTemplate.query("select * from parameters where " + escapeReservedWord("key") + " = ? and reference_type = ? and reference_id = ?"
                    , ORM.getRowMapper()
                    , key
                    , referenceType.name()
                    , referenceId
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find parameter by id", ex);
            throw new TechnicalException("Failed to find parameter by id", ex);
        }
    }


    @Override
    public List<Parameter> findByKeys(List<String> keys, String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findByKeys({}, {}, {})", keys, referenceId, referenceType);
        try {
            if (isEmpty(keys)) {
                return Collections.emptyList();
            }
            List<Parameter> parameters = jdbcTemplate.query("select * from parameters where reference_id = ? and reference_type = ? and " + escapeReservedWord("key") + " in ( "
                            + ORM.buildInClause(keys) + " )"
                    , (PreparedStatement ps) -> {
                        ps.setString(1, referenceId);
                        ps.setString(2, referenceType.name());
                        ORM.setArguments(ps, keys, 3);
                    }
                    , ORM.getRowMapper()
            );
            return new ArrayList<>(parameters);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find parameters by keys:", ex);
            throw new TechnicalException("Failed to find parameters by keys", ex);
        }
    }

    @Override
    public List<Parameter> findAll(String referenceId, ParameterReferenceType referenceType) throws TechnicalException {
        LOGGER.debug("JdbcParameterRepository.findAll({}, {})", referenceId, referenceType);
        try {
            List<Parameter> parameters = jdbcTemplate.query("select * from parameters where reference_id = ? and reference_type = ?"
                    , ORM.getRowMapper()
                    , referenceId
                    , referenceType.name()
            );
            return new ArrayList<>(parameters);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all parameters :", ex);
            throw new TechnicalException("Failed to find all parameters", ex);
        }
    }
}