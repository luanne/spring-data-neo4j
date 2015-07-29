/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.util.Arrays;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.neo4j.integration.conversion.domain.JavaElement;
import org.springframework.data.neo4j.integration.conversion.domain.MonetaryAmount;
import org.springframework.data.neo4j.integration.conversion.domain.PensionPlan;
import org.springframework.data.neo4j.integration.conversion.domain.SiteMember;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Adam George
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { ConversionServicePersistenceContext.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConversionServiceTest {

    @ClassRule
    public static final Neo4jIntegrationTestRule testRule = new Neo4jIntegrationTestRule(7879);

    @Autowired
    private PensionRepository pensionRepository;
    @Autowired
    private JavaElementRepository javaElementRepository;
    @Autowired
    private SiteMemberRepository siteMemberRepository;
    @Autowired
    private GenericConversionService conversionService;

    @After
    public void cleanUpDatabase() {
        testRule.clearDatabase();
    }

    /**
     * This should work by virtue of the fact that there's an OGM-level converter defined on a class we've scanned.
     */
    @Test
    public void shouldBeAbleToConvertBetweenBytesAndBase64EncodedDataViaSpringConversionService() {
        String base64Representation = "YmNkZWY=";
        byte[] binaryData = new byte[] { 98, 99, 100, 101, 102 };

        assertTrue(this.conversionService.canConvert(byte[].class, String.class));
        assertEquals(base64Representation, this.conversionService.convert(binaryData, String.class));

        assertTrue(this.conversionService.canConvert(String.class, byte[].class));
        assertTrue(Arrays.equals(binaryData, this.conversionService.convert(base64Representation, byte[].class)));
    }

    @Test
    public void shouldConvertBase64StringOutOfGraphDatabaseBackIntoByteArray() {
        Result rs = testRule.getGraphDatabaseService().execute(
                "CREATE (u:SiteMember {profilePictureData:'MTIzNDU2Nzg5'}) RETURN id(u) AS userId");
        Long userId = (Long) rs.columnAs("userId").next();

        byte[] expectedData = "123456789".getBytes();

        SiteMember siteMember = this.siteMemberRepository.findOne(userId);
        assertTrue("The data wasn't converted correctly", Arrays.equals(expectedData, siteMember.getProfilePictureData()));
    }

    @Test
    public void shouldConvertFieldsUsingSpringConvertersAddedDirectlyToConversionService() {
        this.conversionService.addConverter(new SpringMonetaryAmountToIntegerConverter());
        this.conversionService.addConverter(new SpringIntegerToMonetaryAmountConverter());

        PensionPlan pensionToSave = new PensionPlan(new MonetaryAmount(16472, 81), "Tightfist Asset Management Ltd");

        this.pensionRepository.save(pensionToSave);

        ResourceIterator<Number> resourceIterator = testRule.getGraphDatabaseService()
                .execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv").columnAs("fv");
        assertTrue("Nothing was saved", resourceIterator.hasNext());
        assertEquals("The amount wasn't converted and persisted correctly", 1647281, resourceIterator.next().intValue());

        PensionPlan reloadedPension = this.pensionRepository.findOne(pensionToSave.getPensionPlanId());
        assertEquals("The amount was converted incorrectly", pensionToSave.getFundValue(), reloadedPension.getFundValue());
    }

    /**
     * If target graph type is set to Integer but we have a converter for a Number then it should still save to the graph.
     */
    @Test
    public void shouldConvertFieldsUsingAnAvailableSupertypeConverterIfExactTypesDoNotMatch() {
        this.conversionService.addConverterFactory(new SpringMonetaryAmountToNumberConverterFactory());

        PensionPlan pension = new PensionPlan(new MonetaryAmount(20_000, 00), "Ashes Assets LLP");
        this.pensionRepository.save(pension);

        ResourceIterator<Integer> resourceIterator = testRule.getGraphDatabaseService()
                .execute("MATCH (p:PensionPlan) RETURN p.fundValue AS fv").columnAs("fv");
        assertTrue("Nothing was saved", resourceIterator.hasNext());
        assertEquals("The amount wasn't converted and persisted correctly", 2000000, resourceIterator.next().intValue());
    }

    /**
     * This should fix DATAGRAPH-659 too.
     */
    @Test
    public void shouldOperateWithDefaultSpringConvertersToConvertObjectTypesNotInMetaData() {
        DefaultConversionService.addDefaultConverters(this.conversionService);

        JavaElement method = new JavaElement();
        method.setName("toString");
        method.setElementType(ElementType.METHOD);

        this.javaElementRepository.save(method);

        ResourceIterator<String> resourceIterator = testRule.getGraphDatabaseService()
                .execute("MATCH (e:JavaElement) RETURN e.elementType AS type").columnAs("type");
        assertTrue("Nothing was saved", resourceIterator.hasNext());
        assertEquals("The element type wasn't converted and persisted correctly", "METHOD", resourceIterator.next());

        JavaElement loadedObject = this.javaElementRepository.findAll().iterator().next();
        assertEquals("The element type wasn't loaded and converted correctly", ElementType.METHOD, loadedObject.getElementType());
    }

}
