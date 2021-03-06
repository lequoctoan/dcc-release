/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.release.job.join.function;

import static org.icgc.dcc.common.core.model.FieldNames.DONOR_SAMPLE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;
import static org.icgc.dcc.release.core.util.FieldNames.JoinFieldNames.BIOMARKER;
import static org.icgc.dcc.release.core.util.FieldNames.JoinFieldNames.SURGERY;
import static org.icgc.dcc.release.job.join.utils.JsonNodes.populateArrayNode;
import lombok.val;

import org.apache.spark.api.java.function.Function;
import org.icgc.dcc.common.core.model.FieldNames;

import scala.Tuple2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;

public class CombineSpecimen implements Function<Tuple2<String, Tuple2<Tuple2<Tuple2<ObjectNode,
    Optional<Iterable<ObjectNode>>>, Optional<Iterable<ObjectNode>>>, Optional<Iterable<ObjectNode>>>>, ObjectNode> {

  @Override
  public ObjectNode call(Tuple2<String, Tuple2<Tuple2<Tuple2<ObjectNode, Optional<Iterable<ObjectNode>>>,
      Optional<Iterable<ObjectNode>>>, Optional<Iterable<ObjectNode>>>> tuple) throws Exception {
    val specimenSampleTuple = tuple._2._1._1;
    val specimen = specimenSampleTuple._1;
    if (specimenSampleTuple._2.isPresent()) {
      val samples = specimen.withArray(DONOR_SAMPLE);
      populateArrayNode(samples, specimenSampleTuple._2.get(), CombineSpecimen::trimSample);
    }

    val biomarkerTuple = tuple._2._1;
    if (biomarkerTuple._2.isPresent()) {
      val biomarker = specimen.withArray(BIOMARKER);
      populateArrayNode(biomarker, biomarkerTuple._2.get(), CombineSpecimen::trimDonorFields);
    }

    val surgeryTuple = tuple._2;
    if (surgeryTuple._2.isPresent()) {
      val surgery = specimen.withArray(SURGERY);
      populateArrayNode(surgery, surgeryTuple._2.get(), CombineSpecimen::trimDonorFields);
    }

    return specimen;
  }

  private static ObjectNode trimSample(ObjectNode node) {
    node.remove(SUBMISSION_SPECIMEN_ID);

    return node;
  }

  private static ObjectNode trimDonorFields(ObjectNode node) {
    node.remove(FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID);
    node.remove(FieldNames.PROJECT_ID);

    return node;
  }

}
