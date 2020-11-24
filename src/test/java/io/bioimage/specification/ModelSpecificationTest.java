/*-
 * #%L
 * This is the bioimage.io modelzoo library for ImageJ.
 * %%
 * Copyright (C) 2019 - 2020 Center for Systems Biology Dresden
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package io.bioimage.specification;

import io.bioimage.specification.transformation.ImageTransformation;
import io.bioimage.specification.transformation.ScaleLinearTransformation;
import io.bioimage.specification.transformation.ZeroMeanUnitVarianceTransformation;
import io.bioimage.specification.weights.TensorFlowSavedModelBundleSpecification;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModelSpecificationTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	// example values
	private final static String source = "trainingSource";
	private final static List<String> modelAuthors = Arrays.asList("author1", "author2");
	private final static String description = "description";
	private final static String documentation = "DOCUMENTATION_LINK";
	private final static String license = "bsd";
	private final static String modelName = "model name";
	private final static Double mean = 100.;
	private final static Double std = 10.;
	private final static String gitRepo = "https://github.com/name/repo";
	private final static String citationText = "Publication name, authors, journal";
	private final static String doi = "DOI";
	private final static List<String> tags = Arrays.asList("tag1", "tag2");
	private final static String inputName = "input";
	private final static String axes = "XYZC";
	private final static List<Integer> shapeMin = Arrays.asList(4, 4, 4, 1);
	private final static List<Integer> shapeStep = Arrays.asList(16, 16, 16, 0);
	private final static List<Integer> halo = Arrays.asList(16, 16, 16, 1);
	private final static List<String> dataRange = Arrays.asList("-inf", "inf");
	private final static String dataType = "float";
	private final static String output = "output";
	private final static List<Integer> shapeOffset = Arrays.asList(0, 0, 0, 3);
	private final static List<Double> shapeScale = Arrays.asList(2., 2., 2., 1.);
	private final static String testInput = "input.png";
	private final static String testOutput = "output.png";
	private final static Map<String, Object> attachments = Collections.singletonMap("manifest", "./manifest/README.txt");
	private final static String weightsSha256 = "1234567";
	private final static String weightsSource = "./weights.zip";
	private final static String timestamp = new Timestamp(System.currentTimeMillis()).toString();
	private final static ImageTransformation.Mode mode = ImageTransformation.Mode.FIXED;

	@Test
	public void testEmptySpec() throws IOException {

		// create spec

		ModelSpecification specification = new DefaultModelSpecification();

		// write spec

		File dir = folder.getRoot();
		specification.write(dir);

		// check if files exist and are not empty

		File modelFile = new File(dir.getAbsolutePath(), specification.getModelFileName());
		assertTrue(modelFile.exists());
		File dependencyFile = new File(dir.getAbsolutePath(), DefaultModelSpecification.dependenciesFileName);
		assertTrue(dependencyFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		assertFalse(content.isEmpty());
		content = FileUtils.readFileToString(dependencyFile, StandardCharsets.UTF_8);
		assertFalse(content.isEmpty());

		// read spec
		ModelSpecification newSpec = new DefaultModelSpecification();
		assertTrue(newSpec.readFromDirectory(dir));

		// check default spec values
		assertEquals(DefaultModelSpecification.modelZooSpecificationVersion, newSpec.getFormatVersion());
	}

	@Test
	public void testExampleSpec() throws IOException {

		// create spec

		File dir = folder.getRoot();
		DefaultModelSpecification specification = new DefaultModelSpecification();
		setExampleValues(specification);

		// check values
		checkExampleValues(specification);

		// write spec
		specification.write(dir);
		File modelFile = new File(dir.getAbsolutePath(), specification.getModelFileName());
		assertTrue(modelFile.exists());
		String content = FileUtils.readFileToString(modelFile, StandardCharsets.UTF_8);
		System.out.println(content);

		// read spec
		ModelSpecification newSpec = new DefaultModelSpecification();
		assertTrue(newSpec.readFromDirectory(dir));

		// check values
		checkExampleValues(newSpec);

	}

	private void setExampleValues(DefaultModelSpecification specification) {
		// meta
		specification.setName(modelName);
		specification.setAuthors(modelAuthors);
		specification.setDescription(description);
		specification.setDocumentation(documentation);
		specification.setLicense(license);
		specification.setSource(source);
		specification.setGitRepo(gitRepo);
		specification.setTags(tags);
		specification.setTimestamp(timestamp);
		CitationSpecification citation = new DefaultCitationSpecification();
		citation.setCitationText(citationText);
		citation.setDOIText(doi);
		specification.addCitation(citation);
		specification.setSampleInputs(Collections.singletonList(testInput));
		specification.setSampleOutputs(Collections.singletonList(testOutput));
		specification.setAttachments(attachments);
		// input node
		InputNodeSpecification inputNode = new DefaultInputNodeSpecification();
		inputNode.setShapeMin(shapeMin);
		inputNode.setShapeStep(shapeStep);
		inputNode.setAxes(axes);
		inputNode.setDataRange(dataRange);
		inputNode.setDataType(dataType);
		inputNode.setName(inputName);
		inputNode.setHalo(halo);
		ZeroMeanUnitVarianceTransformation preprocessing = new ZeroMeanUnitVarianceTransformation();
		preprocessing.setMean(mean);
		preprocessing.setStd(std);
		preprocessing.setMode(mode);
		inputNode.setPreprocessing(Collections.singletonList(preprocessing));
		specification.addInputNode(inputNode);
		// output node
		OutputNodeSpecification outputNode = new DefaultOutputNodeSpecification();
		outputNode.setName(output);
		outputNode.setAxes(axes);
		outputNode.setShapeOffset(shapeOffset);
		outputNode.setShapeReferenceInput(inputName);
		outputNode.setShapeScale(shapeScale);
		ScaleLinearTransformation postprocessing = new ScaleLinearTransformation();
		postprocessing.setGain(std);
		postprocessing.setOffset(mean);
		postprocessing.setMode(mode);
		outputNode.setPostprocessing(Collections.singletonList(postprocessing));
		specification.addOutputNode(outputNode);
		// weights
		TensorFlowSavedModelBundleSpecification weights = new TensorFlowSavedModelBundleSpecification();
		weights.setSha256(weightsSha256);
		weights.setSource(weightsSource);
		specification.addWeights(weights);
	}

	private void checkExampleValues(ModelSpecification specification) {
		// meta
		assertEquals(modelName, specification.getName());
		assertEquals(modelAuthors, specification.getAuthors());
		assertEquals(description, specification.getDescription());
		assertEquals(documentation, specification.getDocumentation());
		assertEquals(license, specification.getLicense());
		assertEquals(source, specification.getSource());
		assertEquals(gitRepo, specification.getGitRepo());
		assertEquals(attachments, specification.getAttachments());
		assertArrayEquals(tags.toArray(), specification.getTags().toArray());
		assertEquals(1, specification.getCitations().size());
		CitationSpecification citation = new DefaultCitationSpecification();
		citation.setCitationText(citationText);
		citation.setDOIText(doi);
		assertEquals(citation, specification.getCitations().get(0));
		assertNotNull(specification.getSampleInputs());
		assertEquals(1, specification.getSampleInputs().size());
		assertEquals(testInput, specification.getSampleInputs().get(0));
		assertNotNull(specification.getSampleOutputs());
		assertEquals(1, specification.getSampleOutputs().size());
		assertEquals(testOutput, specification.getSampleOutputs().get(0));
		assertEquals(timestamp, specification.getTimestamp());

		// input
		assertEquals(1, specification.getInputs().size());
		InputNodeSpecification _input = specification.getInputs().get(0);
		assertEquals(inputName, _input.getName());
		assertEquals(axes, _input.getAxes());
		assertEquals(dataType, _input.getDataType());
		assertArrayEquals(shapeMin.toArray(), _input.getShapeMin().toArray());
		assertArrayEquals(shapeStep.toArray(), _input.getShapeStep().toArray());
		assertArrayEquals(dataRange.toArray(), _input.getDataRange().toArray());
		assertArrayEquals(halo.toArray(), _input.getHalo().toArray());
		assertNotNull(_input.getPreprocessing());
		assertEquals(1, _input.getPreprocessing().size());
		assertEquals(ZeroMeanUnitVarianceTransformation.name, _input.getPreprocessing().get(0).getName());
		ZeroMeanUnitVarianceTransformation preprocessing = (ZeroMeanUnitVarianceTransformation) _input.getPreprocessing().get(0);
		assertEquals(mean, preprocessing.getMean());
		assertEquals(std, preprocessing.getStd());
		assertEquals(mode, preprocessing.getMode());

		// output
		assertEquals(1, specification.getOutputs().size());
		OutputNodeSpecification _output = specification.getOutputs().get(0);
		assertEquals(output, _output.getName());
		assertEquals(axes, _output.getAxes());
		assertEquals(inputName, _output.getReferenceInputName());
		assertArrayEquals(shapeOffset.toArray(), _output.getShapeOffset().toArray());
		assertArrayEquals(shapeScale.toArray(), _output.getShapeScale().toArray());
		assertNotNull(_output.getPostprocessing());
		assertEquals(1, _output.getPostprocessing().size());
		ScaleLinearTransformation postprocessing = (ScaleLinearTransformation) _output.getPostprocessing().get(0);
		assertEquals(ScaleLinearTransformation.name, postprocessing.getName());
		assertEquals(std, postprocessing.getGain());
		assertEquals(mean, postprocessing.getOffset());
		assertEquals(mode, postprocessing.getMode());

		assertNotNull(specification.getWeights());
		assertEquals(1, specification.getWeights().size());
		WeightsSpecification weights = specification.getWeights().get(0);
		assertTrue(weights instanceof TensorFlowSavedModelBundleSpecification);
		assertEquals(weightsSha256, weights.getSha256());
		assertEquals(weightsSource, weights.getSource());
		assertEquals("serve", ((TensorFlowSavedModelBundleSpecification)weights).getTag());
	}

}