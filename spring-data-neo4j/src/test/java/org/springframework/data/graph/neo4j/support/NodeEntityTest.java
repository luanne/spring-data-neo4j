package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Group;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})

    public class NodeEntityTest {

	protected final Log log = LogFactory.getLog(getClass());

	@Autowired
	private GraphDatabaseContext graphDatabaseContext;

	@Autowired
	private FinderFactory finderFactory;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void testUserConstructor() {
        Person p = persistedPerson("Rod", 39);
        assertEquals(p.getName(), p.getPersistentState().getProperty("Person.name"));
        assertEquals(p.getAge(), p.getPersistentState().getProperty("Person.age"));
        Person found = graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(p.getNodeId()), Person.class);
        assertEquals("Rod", found.getPersistentState().getProperty("Person.name"));
        assertEquals(39, found.getPersistentState().getProperty("Person.age"));
    }

    @Test
    @Transactional
    public void testSetProperties() {
        Person p = persistedPerson("Foo", 2);
        p.setName("Michael");
        p.setAge(35);
        p.setHeight((short)182);
        assertEquals("Michael", p.getPersistentState().getProperty("Person.name"));
        assertEquals(35, p.getPersistentState().getProperty("Person.age"));
        assertEquals((short)182, p.getPersistentState().getProperty("Person.height"));
        assertEquals((short)182, (short)p.getHeight());
    }
    @Test
    @Transactional
    public void testSetShortProperty() {
        Group group = new Group().persist();
        group.setName("developers");
        assertEquals("developers", group.getPersistentState().getProperty("name"));
    }
    // own transaction handling because of http://wiki.neo4j.org/content/Delete_Semantics
    @Test(expected = NotFoundException.class)
    public void testDeleteEntityFromGDC() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        graphDatabaseContext.removeNodeEntity(spouse);
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index",spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteEntity() {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = persistedPerson("Michael", 35);
        Person spouse = persistedPerson("Tina", 36);
        p.setSpouse(spouse);
        long id = spouse.getId();
        spouse.remove();
        tx.success();
        tx.finish();
        Assert.assertNull("spouse removed " + p.getSpouse(), p.getSpouse());
        NodeFinder<Person> finder = finderFactory.createNodeEntityFinder(Person.class);
        Person spouseFromIndex = finder.findByPropertyValue(Person.NAME_INDEX, "name", "Tina");
        Assert.assertNull("spouse not found in index", spouseFromIndex);
        Assert.assertNull("node deleted " + id, graphDatabaseContext.getNodeById(id));
    }

}