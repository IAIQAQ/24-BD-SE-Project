package cn.edu.hust.mathdrill.admin;
import cn.edu.hust.mathdrill.auth.AuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service public class ImportService {
 private static final String[] HEADERS={"题干","年级","学期","知识点","答案类型","标准答案","解析"};
 private final JdbcTemplate db;private final AuthService auth;private final AdminService admin;private final ObjectMapper json;
 public ImportService(JdbcTemplate db,AuthService auth,AdminService admin,ObjectMapper json){this.db=db;this.auth=auth;this.admin=admin;this.json=json;ZipSecureFile.setMaxEntrySize(20*1024*1024);}
 private long owner(Authentication a){return Long.parseLong(auth.requireRole(a,"ADMIN").id());}
 private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
 private Map<String,Object> decode(String value){try{return json.readValue(value,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
 private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_IMPORT: "+text);}
 public byte[] template(Authentication a){owner(a);try(var book=new XSSFWorkbook();var output=new ByteArrayOutputStream()){var sheet=book.createSheet("题库");var row=sheet.createRow(0);for(int i=0;i<HEADERS.length;i++){row.createCell(i).setCellValue(HEADERS[i]);sheet.setColumnWidth(i,24*256);}book.write(output);return output.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
 private Map<String,Object> report(String id,String state,int total,List<Map<String,Object>> issues){var badRows=new HashSet<Integer>();for(var issue:issues)badRows.add(((Number)issue.get("row")).intValue());var blocked=new ArrayList<Integer>();if(state.equals("REJECTED"))for(int r=2;r<total+2;r++)if(!badRows.contains(r))blocked.add(r);return Map.of("id",id,"state",state,"total",total,"success",state.equals("IMPORTED")?total:0,"notImported",state.equals("IMPORTED")?0:total,"issues",issues,"blockedRows",blocked);}
 @Transactional public Object upload(Authentication a,MultipartFile file){long owner=owner(a);if(file.isEmpty()||file.getSize()>5*1024*1024||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw bad("请上传5MB以内的.xlsx文件");
  byte[] bytes;String hash;try{bytes=file.getBytes();hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw bad("无法读取文件");}
  var existing=db.queryForList("SELECT report_json FROM import_batch WHERE owner_id=? AND payload_hash=?",owner,hash);if(!existing.isEmpty())return decode(existing.getFirst().get("report_json").toString());
  List<AdminService.QuestionInput> inputs=new ArrayList<>();var issues=new ArrayList<Map<String,Object>>();int total=0;var seen=new HashSet<String>();
  try(var book=WorkbookFactory.create(new ByteArrayInputStream(bytes))){if(book.getNumberOfSheets()!=1)throw bad("请使用只有一张工作表的模板");var sheet=book.getSheetAt(0);if(sheet.getLastRowNum()>500)throw bad("单批最多500行");var formatter=new DataFormatter(Locale.ROOT);var header=sheet.getRow(0);if(header==null)throw bad("缺少业务列名");var columns=new HashMap<String,Integer>();for(var cell:header){String title=formatter.formatCellValue(cell).trim();if(columns.put(title,cell.getColumnIndex())!=null)throw bad("列名重复："+title);}for(String title:HEADERS)if(!columns.containsKey(title))throw bad("缺少列："+title);
   for(int n=1;n<=sheet.getLastRowNum();n++){var row=sheet.getRow(n);if(row==null)continue;String[] values=new String[7];boolean blank=true,formula=false;for(int c=0;c<7;c++){var cell=row.getCell(columns.get(HEADERS[c]));if(cell!=null&&cell.getCellType()==CellType.FORMULA)formula=true;values[c]=formatter.formatCellValue(cell).trim();if(!values[c].isEmpty())blank=false;}if(blank)continue;total++;String column="题干/分类/答案";
    try{if(formula)throw bad("不可用公式代替题目或标准答案");for(int c=0;c<7;c++)if(values[c].isBlank()){column=HEADERS[c];throw bad("必填信息缺失");}column="年级";int grade=Integer.parseInt(values[1]);if(grade<1||grade>3)throw bad("仅支持1—3年级");column="学期";if(!Set.of("上册","下册").contains(values[2]))throw bad("学期需为上册或下册");column="答案类型";if(!values[4].equals("整数"))throw bad("本阶段仅启用整数答案");column="知识点";
      var points=db.queryForList("SELECT id FROM knowledge_point WHERE name=? AND grade=? AND ((semester=? ) OR (?='上册' AND semester LIKE '上学期%'))",values[3],grade,values[2],values[2]);if(points.size()!=1)throw bad("知识点不存在或分类不唯一，请先维护分类");
      column="标准答案";if(!values[5].matches("[0-9]+"))throw bad("标准答案需为非负整数文本");if(values[0].length()>200||values[5].length()>128||values[6].length()>500)throw bad("内容长度超限");var input=new AdminService.QuestionInput(points.getFirst().get("id").toString(),values[0],values[5],values[6]);column="题干/标准答案";admin.validate(input,null);
      String signature=values[0].replaceAll("\\s+","").replace('＋','+').replace('－','-').replace('−','-').replace('×','*').replace('÷','/')+"|"+new java.math.BigInteger(values[5]);if(!seen.add(signature))throw bad("文件内有重复题");inputs.add(input);
    }catch(RuntimeException e){issues.add(Map.of("row",n+1,"column",column,"reason",e instanceof ResponseStatusException r?Objects.toString(r.getReason(),"格式不正确"):"格式不正确"));}
   }
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw bad("Excel无法解析，请重新使用模板保存");}
  if(total==0)throw bad("没有题目数据");String id=UUID.randomUUID().toString(),state=issues.isEmpty()?"READY":"REJECTED";var report=report(id,state,total,issues);db.update("INSERT INTO import_batch(id,owner_id,payload_hash,state,payload_json,report_json) VALUES(?,?,?,?,?,?)",id,owner,hash,state,encode(inputs),encode(report));return report;
 }
 @Transactional public Object status(Authentication a,String id){long owner=owner(a);var rows=db.queryForList("SELECT report_json FROM import_batch WHERE id=? AND owner_id=?",id,owner);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这次导入");return decode(rows.getFirst().get("report_json").toString());}
 @Transactional public Object confirm(Authentication a,String id,boolean confirm){long owner=owner(a);if(!confirm)throw bad("请确认本批题目校验结果后导入");var rows=db.queryForList("SELECT * FROM import_batch WHERE id=? AND owner_id=? FOR UPDATE",id,owner);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"FORBIDDEN: 无法访问这次导入");var batch=rows.getFirst();if(!batch.get("state").equals("READY"))return decode(batch.get("report_json").toString());
  List<AdminService.QuestionInput> inputs;try{inputs=json.readValue(batch.get("payload_json").toString(),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}var issues=new ArrayList<Map<String,Object>>();for(int i=0;i<inputs.size();i++)try{admin.validate(inputs.get(i),null);}catch(ResponseStatusException e){issues.add(Map.of("row",i+2,"column","题干/标准答案/知识点","reason",Objects.toString(e.getReason(),"校验失败")));}
  if(!issues.isEmpty()){var report=report(id,"REJECTED",inputs.size(),issues);db.update("UPDATE import_batch SET state='REJECTED',report_json=? WHERE id=?",encode(report),id);return report;}
  for(var input:inputs)admin.question(a,null,input);var report=report(id,"IMPORTED",inputs.size(),List.of());db.update("UPDATE import_batch SET state='IMPORTED',report_json=? WHERE id=?",encode(report),id);return report;
 }
}
