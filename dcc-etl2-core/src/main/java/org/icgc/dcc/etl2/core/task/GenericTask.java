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
package org.icgc.dcc.etl2.core.task;

import static org.icgc.dcc.common.core.util.FormatUtils.formatBytes;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.mapred.JobConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.rdd.HadoopPartition;
import org.icgc.dcc.etl2.core.function.FormatObjectNode;
import org.icgc.dcc.etl2.core.function.ParseObjectNode;
import org.icgc.dcc.etl2.core.job.FileType;
import org.icgc.dcc.etl2.core.util.JavaRDDs;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
public abstract class GenericTask extends NamedTask {

  public GenericTask(String name) {
    super(name);
  }

  protected JobConf createJobConf(TaskContext taskContext) {
    val sparkContext = taskContext.getSparkContext();

    return new JobConf(sparkContext.hadoopConfiguration());
  }

  protected JavaRDD<ObjectNode> readInput(TaskContext taskContext, FileType inputFileType) {
    val hadoopConf = createJobConf(taskContext);

    return readInput(taskContext, hadoopConf, inputFileType);
  }

  protected JavaRDD<ObjectNode> readInput(TaskContext taskContext, JobConf hadoopConf, FileType inputFileType) {
    val sparkContext = taskContext.getSparkContext();

    return JavaRDDs.javaTextFile(sparkContext, taskContext.getPath(inputFileType), hadoopConf)
        .map(new ParseObjectNode());
  }

  protected void writeOutput(TaskContext taskContext, JavaRDD<ObjectNode> processed, FileType outputFileType) {
    val output = processed.map(new FormatObjectNode());
    val outputPath = taskContext.getPath(outputFileType);
    output.saveAsTextFile(outputPath);
  }

  @SneakyThrows
  protected static void logPartitions(JavaRDD<?> rdd) {
    val partitions = rdd.partitions();
    for (int i = 0; i < partitions.size(); i++) {
      val partition = (HadoopPartition) partitions.get(i);
      log.info("[{}/{}] Input split ({}): {}",
          i + 1,
          partitions.size(),
          formatBytes(partition.inputSplit().value().getLength()),
          partition.inputSplit());
    }
  }

}