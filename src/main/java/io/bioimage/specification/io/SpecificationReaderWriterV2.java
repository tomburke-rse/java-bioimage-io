/*-
 * #%L
 * Java implementation of the bioimage.io model specification.
 * %%
 * Copyright (C) 2020 - 2021 Center for Systems Biology Dresden
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
package io.bioimage.specification.io;

import io.bioimage.specification.*;
import io.bioimage.specification.transformation.ImageTransformation;
import io.bioimage.specification.transformation.ScaleLinearTransformation;
import io.bioimage.specification.transformation.ZeroMeanUnitVarianceTransformation;
import io.bioimage.specification.weights.TensorFlowSavedModelBundleSpecification;

import java.util.*;
import java.util.stream.Collectors;

import static io.bioimage.specification.util.SpecificationUtil.asMap;

class SpecificationReaderWriterV2 {

    private final static String idName = "name";
    private final static String idDescription = "description";
    private final static String idCite = "cite";
    private final static String idAuthors = "authors";
    private final static String idDocumentation = "documentation";
    private final static String idTags = "tags";
    private final static String idLicense = "license";
    private final static String idFormatVersion = "format_version";

    private final static String idLanguage = "language";

    private final static String idFramework = "framework";
    private final static String idSource = "source";
    private final static String idTestInput = "test_input";
    private final static String idTestOutput = "test_output";
    private final static String idInputs = "inputs";
    private final static String idOutputs = "outputs";
    private final static String idPrediction = "prediction";
    private final static String idTraining = "training";
    private final static String idTrainingSource = "source";
    private final static String idTrainingKwargs = "kwargs";

    private final static String idNodeName = "name";
    private final static String idNodeAxes = "axes";
    private final static String idNodeDataType = "data_type";
    private final static String idNodeDataRange = "data_range";
    private final static String idNodeShape = "shape";
    private final static String idNodeHalo = "halo";

    private final static String idNodeShapeMin = "min";
    private final static String idNodeShapeStep = "step";
    private final static String idNodeShapeReferenceInput = "reference_input";
    private final static String idNodeShapeScale = "scale";
    private final static String idNodeShapeOffset = "offset";

    private final static String idCiteText = "text";
    private final static String idCiteDoi = "doi";

    private final static String idPredictionPreprocess = "preprocess";
    private final static String idTransformationKwargs = "kwargs";
    private final static String idTransformationMean = "mean";
    private final static String idTransformationStd = "stdDev";
    private final static String idFijiConfig = "fiji";

    static ModelSpecification read(DefaultModelSpecification specification, Map<String, Object> obj) {
        readMeta(specification, obj);
        readInputsOutputs(specification, obj);
        readTraining(specification, obj);
        readPrediction(specification, obj);
        WeightsSpecification weights = new TensorFlowSavedModelBundleSpecification();
        weights.setSource(null);
        specification.addWeights(TensorFlowSavedModelBundleSpecification.id, weights);
        return specification;
    }

    private static void readMeta(DefaultModelSpecification specification, Map<String, Object> obj) {
        specification.setName((String) obj.get(idName));
        specification.setDescription((String) obj.get(idDescription));
        if (obj.get(idCite) != null && List.class.isAssignableFrom(obj.get(idCite).getClass())) {
            List<Map> citations = (List<Map>) obj.get(idCite);
            for (Map citation : citations) {
                specification.addCitation(readCitation(citation));
            }
        }
        Object authors = obj.get(idAuthors);
        if (authors != null) {
            if (List.class.isAssignableFrom(authors.getClass())) {
                if(!((List<?>) authors).isEmpty() && String.class.isAssignableFrom(((List<?>) authors).get(0).getClass())){
                    List<AuthorSpecification> authorsList = ((List<String>) authors).stream().map(name->{
                        AuthorSpecification author = new DefaultAuthorSpecification();
                        author.setName(name);
                        return author;
                    }).collect(Collectors.toList());
                    specification.setAuthors(authorsList);
                }else{
                    specification.setAuthors((List<AuthorSpecification>) authors);
                }

            } else if (String.class.isAssignableFrom(authors.getClass())) {
                AuthorSpecification author = new DefaultAuthorSpecification();
                author.setName((String) authors);
                specification.setAuthors(Arrays.asList(author));
            }
        }
        specification.setDocumentation((String) obj.get(idDocumentation));
        specification.setTags((List<String>) obj.get(idTags));
        specification.setLicense((String) obj.get(idLicense));
        specification.setFormatVersion((String) obj.get(idFormatVersion));
        specification.setSource((String) obj.get(idSource));
        specification.setExecutionModel((String) getExecutionModel(obj));
        specification.setSampleInputs(Collections.singletonList((String) obj.get(idTestInput)));
        specification.setSampleOutputs(Collections.singletonList((String) obj.get(idTestOutput)));
    }

    private static Object getExecutionModel(Map<String, Object> obj) {
        Object source = obj.get(idSource);
        if (source == null) return null;
        if (source.equals("denoiseg")) return source;
        return null;
    }

    private static void readInputsOutputs(DefaultModelSpecification specification, Map<String, Object> obj) {
        List<Map> inputs = (List<Map>) obj.get(idInputs);
        for (Map input : inputs) {
            specification.addInputNode(readInputNode(input));
        }
        List<Map> outputs = (List<Map>) obj.get(idOutputs);
        for (Map output : outputs) {
            specification.addOutputNode(readOutputNode(output));
        }
    }

    private static void readTraining(DefaultModelSpecification specification, Map<String, Object> obj) {
        Map<String, Object> training = asMap(obj.get(idTraining));
        if (training == null) return;
        String trainingSource = (String) training.get(idTrainingSource);
        Map<String, Object> trainingKwargs = asMap(training.get(idTrainingKwargs));
        Map<String, Object> config = new HashMap<>();
        training.put(idTrainingSource, trainingSource);
        training.put(idTrainingKwargs, trainingKwargs);
        Map<String, Object> fijiConfig = new HashMap<>();
        fijiConfig.put(idTraining, training);
        config.put(idFijiConfig, fijiConfig);
        specification.setConfig(config);
    }

    private static void readPrediction(ModelSpecification specification, Map<String, Object> obj) {
        Map<String, Object> prediction = asMap(obj.get(idPrediction));
        if (prediction == null) return;
        List allpreprocess = (List) prediction.get(idPredictionPreprocess);
        if (allpreprocess == null || allpreprocess.size() == 0) return;
        Map<String, Object> preprocess = asMap(allpreprocess.get(0));
        Map<String, Object> kwargs = asMap(preprocess.get(idTransformationKwargs));
        if (kwargs == null) return;
        List stdList = (List) kwargs.get(idTransformationStd);
        List meanList = (List) kwargs.get(idTransformationMean);
        if (meanList == null || meanList.size() == 0 || stdList == null || stdList.size() == 0) return;
        ZeroMeanUnitVarianceTransformation pre = new ZeroMeanUnitVarianceTransformation();
        pre.setStd((Number) stdList.get(0));
        pre.setMean((Number) meanList.get(0));
        pre.setMode(ImageTransformation.Mode.FIXED);
        ScaleLinearTransformation post = new ScaleLinearTransformation();
        post.setOffset(pre.getMean());
        post.setGain(pre.getStd());
        post.setMode(ImageTransformation.Mode.FIXED);
        specification.getInputs().get(0).setPreprocessing(Collections.singletonList(pre));
        if (!specification.getSource().equals("denoiseg")) {
            specification.getOutputs().get(0).setPostprocessing(Collections.singletonList(post));
        }
    }

    private static InputNodeSpecification readInputNode(Map data) {
        InputNodeSpecification node = new DefaultInputNodeSpecification();
        readNode(node, data);
        Map<String, Object> shapeData = asMap(data.get(idNodeShape));
        node.setShapeMin((List<Integer>) shapeData.get(idNodeShapeMin));
        node.setShapeStep((List<Integer>) shapeData.get(idNodeShapeStep));
        return node;
    }

    private static OutputNodeSpecification readOutputNode(Map data) {
        OutputNodeSpecification node = new DefaultOutputNodeSpecification();
        readNode(node, data);
        Map<String, Object> shapeData = asMap(data.get(idNodeShape));
        node.setShapeReferenceInput((String) shapeData.get(idNodeShapeReferenceInput));
        node.setShapeScale((List<Number>) shapeData.get(idNodeShapeScale));
        node.setShapeOffset((List<Integer>) shapeData.get(idNodeShapeOffset));
        return node;
    }

    private static void readNode(NodeSpecification node, Map data) {
        node.setName((String) data.get(idNodeName));
        node.setAxes((String) data.get(idNodeAxes));
        node.setDataType((String) data.get(idNodeDataType));
        node.setDataRange((List<?>) data.get(idNodeDataRange));
        node.setHalo((List<Integer>) data.get(idNodeHalo));
    }

    private static CitationSpecification readCitation(Map data) {
        CitationSpecification citation = new DefaultCitationSpecification();
        citation.setCitationText((String) data.get(idCiteText));
        citation.setDOIText((String) data.get(idCiteDoi));
        return citation;
    }

    static Map<String, Object> write(ModelSpecification specification) {
        Map<String, Object> data = new LinkedHashMap<>();
        writeMeta(specification, data);
        writeInputsOutputs(specification, data);
        writeTraining(specification, data);
        writePrediction(specification, data);
        return data;
    }

    private static void writeInputsOutputs(ModelSpecification specification, Map<String, Object> data) {
        data.put(idInputs, buildInputList(specification));
        data.put(idOutputs, buildOutputList(specification));
    }

    private static void writeMeta(ModelSpecification specification, Map<String, Object> data) {
        data.put(idFormatVersion, specification.getFormatVersion());
        data.put(idName, specification.getName());
        data.put(idDescription, specification.getDescription());
        data.put(idAuthors, specification.getAuthors());
        data.put(idCite, buildCitationList(specification));
        data.put(idDocumentation, specification.getDocumentation());
        data.put(idTags, specification.getTags());
        data.put(idLicense, specification.getLicense());
        data.put(idSource, specification.getSource());
        if (specification.getSampleInputs() != null && specification.getSampleInputs().size() > 0) {
            data.put(idTestInput, specification.getSampleInputs().get(0));
        }
        if (specification.getSampleOutputs() != null && specification.getSampleOutputs().size() > 0) {
            data.put(idTestOutput, specification.getSampleOutputs().get(0));
        }
    }

    private static List<Map<String, Object>> buildInputList(ModelSpecification specification) {
        List<Map<String, Object>> inputs = new ArrayList<>();
        if (specification.getInputs() != null) {
            for (InputNodeSpecification input : specification.getInputs()) {
                inputs.add(writeInputNode(input));
            }
        }
        return inputs;
    }

    private static List<Map<String, Object>> buildOutputList(ModelSpecification specification) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        if (specification.getOutputs() != null) {
            for (OutputNodeSpecification output : specification.getOutputs()) {
                outputs.add(writeOutputNode(output));
            }
        }
        return outputs;
    }

    private static List<Map<String, Object>> buildCitationList(ModelSpecification specification) {
        List<Map<String, Object>> cite = new ArrayList<>();
        if (specification.getCitations() != null) {
            for (CitationSpecification citation : specification.getCitations()) {
                cite.add(writeCitation(citation));
            }
        }
        return cite;
    }

    private static Map<String, Object> writeNode(NodeSpecification node) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put(idNodeName, node.getName());
        if (node.getAxes() != null) res.put(idNodeAxes, node.getAxes());
        if (node.getDataType() != null) res.put(idNodeDataType, node.getDataType());
        if (node.getDataRange() != null) res.put(idNodeDataRange, node.getDataRange());
        if (node.getHalo() != null) res.put(idNodeHalo, node.getHalo());
        return res;
    }

    private static Map<String, Object> writeInputNode(InputNodeSpecification node) {
        Map<String, Object> res = writeNode(node);
        Map<String, Object> shape = new HashMap<>();
        if (node.getShapeMin() != null) shape.put(idNodeShapeMin, node.getShapeMin());
        if (node.getShapeStep() != null) shape.put(idNodeShapeStep, node.getShapeStep());
        res.put(idNodeShape, shape);
        return res;
    }

    private static Map<String, Object> writeOutputNode(OutputNodeSpecification node) {
        Map<String, Object> res = writeNode(node);
        Map<String, Object> shape = new HashMap<>();
        shape.put(idNodeShapeReferenceInput, node.getReferenceInputName());
        shape.put(idNodeShapeScale, node.getShapeScale());
        shape.put(idNodeShapeOffset, node.getShapeOffset());
        res.put(idNodeShape, shape);
        return res;
    }

    private static Map<String, Object> writeCitation(CitationSpecification citation) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put(idCiteText, citation.getCitationText());
        res.put(idCiteDoi, citation.getDoiText());
        return res;
    }

    private static void writePrediction(ModelSpecification specification, Map<String, Object> data) {
        if (specification.getInputs().size() != 1
                || specification.getInputs().get(0).getPreprocessing() == null
                || specification.getInputs().get(0).getPreprocessing().size() != 1
                || specification.getInputs().get(0).getPreprocessing().size() != 1) {
            return;
        }
        TransformationSpecification transformation = specification.getInputs().get(0).getPreprocessing().get(0);
        if (!(transformation instanceof ZeroMeanUnitVarianceTransformation)) {
            return;
        }
        ZeroMeanUnitVarianceTransformation zeroMean = (ZeroMeanUnitVarianceTransformation) transformation;
        Map<String, Object> prediction = new LinkedHashMap<>();
        Map<String, Object> kwargs = new HashMap<>();
        kwargs.put(idTransformationMean, Collections.singletonList(zeroMean.getMean()));
        kwargs.put(idTransformationStd, Collections.singletonList(zeroMean.getStd()));
        Map<String, Object> transform = new HashMap<>();
        transform.put(idTrainingKwargs, kwargs);
        prediction.put(idPredictionPreprocess, Collections.singletonList(transform));
        data.put(idPrediction, prediction);
    }

    private static void writeTraining(ModelSpecification specification, Map<String, Object> data) {
        Map<String, Object> config = specification.getConfig();
        if (config == null) return;
        Map<String, Object> fiji = (Map<String, Object>) config.get("fiji");
        if (fiji == null) return;
        data.put(idTraining, fiji.get(idTraining));
    }

    public static boolean canRead(Map<String, Object> obj) {
        String version = (String) obj.get(idFormatVersion);
        return Objects.equals(version, "0.2.1-csbdeep")
                || Objects.equals(version, "0.2.0-csbdeep");
    }

    static boolean canWrite(ModelSpecification specification) {
        String version = specification.getFormatVersion();
        return Objects.equals(version, "0.2.1-csbdeep")
                || Objects.equals(version, "0.2.0-csbdeep");
    }
}
