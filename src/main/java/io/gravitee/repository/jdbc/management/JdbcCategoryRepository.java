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
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.*;

import static io.gravitee.repository.jdbc.common.AbstractJdbcRepositoryConfiguration.escapeReservedWord;

/**
 *
 * @author njt
 */
@Repository
public class JdbcCategoryRepository implements CategoryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcCategoryRepository.class);

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private static final JdbcObjectMapper ORM = JdbcObjectMapper.builder(Category.class, "categories", "id")
            .addColumn("id", Types.NVARCHAR, String.class)
            .addColumn("environment_id", Types.NVARCHAR, String.class)
            .addColumn("key", Types.NVARCHAR, String.class)
            .addColumn("name", Types.NVARCHAR, String.class)
            .addColumn("description", Types.NVARCHAR, String.class)
            .addColumn("hidden", Types.BIT, boolean.class)
            .addColumn("order", Types.INTEGER, int.class)
            .addColumn("highlight_api", Types.NVARCHAR, String.class)
            .addColumn("picture", Types.NVARCHAR, String.class)
            .addColumn("created_at", Types.TIMESTAMP, Date.class)
            .addColumn("updated_at", Types.TIMESTAMP, Date.class)
            .addColumn("background", Types.NVARCHAR, String.class)
            .addColumn("page", Types.NVARCHAR, String.class)
            .build();

    @Override
    public Set<Category> findAllByEnvironment(String environmentId) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.findAllByEnvironment({})", environmentId);
        try {
            List<Category> categories = jdbcTemplate.query("select * from categories where environment_id = ?"
                    , ORM.getRowMapper()
                    , environmentId
            );
            return new HashSet<>(categories);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find categories by environment:", ex);
            throw new TechnicalException("Failed to find categories by environment", ex);
        }
    }

    @Override
    public Optional<Category> findById(String id) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.findById({})", id);
        try {
            List<Category> items = jdbcTemplate.query("select * from categories where id = ?"
                    , ORM.getRowMapper()
                    , id
            );
            return items.stream().findFirst();
        } catch (final Exception ex) {
            LOGGER.error("Failed to find categories items by id : {} ", id, ex);
            throw new TechnicalException("Failed to find categories items by id : " + id, ex);
        }
    }

    @Override
    public Category create(Category item) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.create({})", item);
        try {
            jdbcTemplate.update(ORM.buildInsertPreparedStatementCreator(item));
            return findById(item.getId()).orElse(null);
        } catch (final Exception ex) {
            LOGGER.error("Failed to create categories item:", ex);
            throw new TechnicalException("Failed to create categories item.", ex);
        }
    }

    @Override
    public Category update(Category item) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.update({})", item);
        if (item == null) {
            throw new IllegalStateException("Unable to update null item");
        }
        try {
            int rows = jdbcTemplate.update("update categories set "
                                        + " id = ?"
                                        + " , environment_id = ?"
                                        + " , " + escapeReservedWord("key") + " = ?"
                                        + " , name = ?"
                                        + " , description = ?"
                                        + " , hidden = ?"
                                        + " , " + escapeReservedWord("order") + " = ?"
                                        + " , highlight_api = ?"
                                        + " , picture = ?"
                                        + " , background = ?"
                                        + " , page = ?"
                                        + " , created_at = ? "
                                        + " , updated_at = ? "
                                        + " where "
                                        + " id = ? "
                                        + " and environment_id = ? "

                                , item.getId()
                                , item.getEnvironmentId()
                                , item.getKey()
                                , item.getName()
                                , item.getDescription()
                                , item.isHidden()
                                , item.getOrder()
                                , item.getHighlightApi()
                                , item.getPicture()
                                , item.getBackground()
                                , item.getPage()
                                , item.getCreatedAt()
                                , item.getUpdatedAt()
                                , item.getId()
                                , item.getEnvironmentId()
                        );
            if (rows == 0) {
                throw new IllegalStateException("Unable to update categories " + item.getId() + " for the environment " + item.getEnvironmentId());
            } else {
                return findById(item.getId()).orElse(null);
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (final Exception ex) {
            LOGGER.error("Failed to update categories item:", ex);
            throw new TechnicalException("Failed to update categories item", ex);
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.delete({})", id);
        try {
            jdbcTemplate.update("delete from categories where id = ?", id);
        } catch (final Exception ex) {
            LOGGER.error("Failed to delete categories item:", ex);
            throw new TechnicalException("Failed to delete categories item", ex);
        }
        
    }

    @Override
    public Set<Category> findAll() throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.findAll()");
        try {
            List<Category> items = jdbcTemplate.query(ORM.getSelectAllSql(), ORM.getRowMapper());
            return new HashSet<>(items);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find all categories items:", ex);
            throw new TechnicalException("Failed to find all categories items", ex);
        }
    }
    
    @Override
    public Optional<Category> findByKey(String key, String environment) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.findByKey({},{})", key, environment);
        try {
            final Optional<Category> category = jdbcTemplate.query(
                    "select * from categories where " + escapeReservedWord("key") + " = ? and environment_id = ?", ORM.getRowMapper(), key, environment)
                    .stream().findFirst();
            return category;
        } catch (final Exception ex) {
            final String error = "Failed to find category by key " + key;
            LOGGER.error(error, ex);
            throw new TechnicalException(error, ex);
        }
    }

    @Override
    public Set<Category> findByPage(String page) throws TechnicalException {
        LOGGER.debug("JdbcCategoryRepository.findByPage()");
        try {
            List<Category> categories = jdbcTemplate.query("select * from categories where page = ?"
                    , ORM.getRowMapper()
                    , page
            );
            return new HashSet<>(categories);
        } catch (final Exception ex) {
            LOGGER.error("Failed to find categories by page:", ex);
            throw new TechnicalException("Failed to find categories by page", ex);
        }
    }
}
