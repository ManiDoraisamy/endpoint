<%@ page import="com.guesswork.community.endpoint.*" %>
<!DOCTYPE html>
<html lang="en">
<head>
	<title>Simple Endpoint for Google App engine</title>
    <link href="bootstrap/css/bootstrap.css" rel="stylesheet">
    <style>
    	#dataset { margin-top:50px; padding-left:0px; padding-right:0px; }
    	#dataset table { width:100%; }
    	#dataset table tr td { padding:5px; }
    	#dataset table tr td .update{ width:80px; }
    	#dataset table tr td .create{ width:105px; }
    	#dataset table tr:first-child { background-color: #f6f4eb; }
    	#dataset table tr:first-child td { font-size:17px; font-weight:500; padding:10px; }
    	#dataset table tr:nth-child(odd) { background-color: #f6f4eb; }
    	#admodal { }
    	#admodal textarea { width:100%; height:120px;}
    </style>
</head>
<body id="content">
</body>
<script src="js/json2.js"></script>
<script src="js/jquery.min.js"></script>
<script src="js/ejs_production.js"></script>
<script src="bootstrap/js/bootstrap.min.js"></script>
<script id="template" type="ejs">
    <div class="navbar navbar-default navbar-fixed-top">
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="#">Simple Endpoint</a>
        </div>
        <div class="navbar-collapse collapse">
          <ul class="nav navbar-nav">
			[% for(var k in kinds.rows) { %]
			[% var kind = kinds.rows[k] %]
            <li class="[%=selected==k?"active":""%]"><a href="#" onClick="explorer.load([%=k%])">[%=kind.id%]</a></li>
			[% } %]
          </ul>
          <ul class="nav navbar-nav navbar-right">
            <li><div style="margin-top:8px;">
				<a href="#!" class="btn btn-default" onClick="explorer.execute('create')">New Endpoint</a>
			<div></li>
          </ul>
        </div>
      </div>
    </div>

    <div id="dataset" class="container-fluid">
		[% var kind = kinds.rows[selected] %]
		[% if(kind) { %]
		<table class="dataset">
		<tbody>
			<tr>
				<td>Id</td>
				[% for(var col in kind.columns) { %]
				<td>[%=col%]</td>
				[% } %]
				<td></td>
			</tr>
			[% for(var i = 0; i < data.rows.length; i++) { %]
			<tr>
				[% var row = data.rows[i] %]
				<td>[%=row.id%]</td>
				[% for(var col in kind.columns) { %]
				<td>[%=row[col]%]</td>
				[% } %]
				<td>
	<div class="btn-group pull-right">
        <button type="button" class="btn btn-default update" onClick="explorer.execute('update',explorer.selected,[%=i%])">Update</button>
        <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown"><span class="caret"></span></button>
        <ul class="dropdown-menu" role="menu">
          <li><a href="#!" onClick="explorer.execute('read',explorer.selected,[%=i%])">Read</a></li>
          <li><a href="#!" onClick="explorer.execute('update',explorer.selected,[%=i%])">Update</a></li>
          <li><a href="#!" onClick="explorer.execute('delete',explorer.selected,[%=i%])">Delete</a></li>
        </ul>
	</div>
				</td>
			</tr>
			[% } %]
			<tr>
				<td>*</td>
				[% for(var col in kind.columns) { %]
				<td></td>
				[% } %]
				<td>
				<a href="#!" class="btn btn-default pull-right create" onClick="explorer.execute('create',explorer.selected)">
					Create
				</a>
				</td>
			</tr>
		<tbody>
		</table>
		[% } %]
    </div>

	<div class="modal fade" id="admodal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  		<div class="modal-dialog">
    		<div class="modal-content" id="admodal-content">
    		</div>
 		</div>
	</div>
</script>
<script id="dialog" type="ejs">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="myModalLabel">[%=action.type%] [%=action.meta?action.meta.id:"new kind"%]</h4>
      </div>
      <div class="modal-body">
		[%=types[action.type]%]
		[% if(action.meta) { %]
			[%=action.url%]
		[% } else { %]
			[%=action.url%]<input id="newkind" type="text" placeholder="new kind"/>
		[% } %]
		<br/><br/>
		<textarea id="jsontxt">[%=JSON.stringify(action.instance?action.instance:{})%]</textarea>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-primary" data-dismiss="modal"
			onClick="explorer.save(jQuery('#jsontxt').val(), jQuery('#newkind').val())">Send</button>
        <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
      </div>
</script>
<script>
function mExplorer()
{
	this.selected = 0;
	this.kinds = {};
	this.data = {};
	this.action = {};
	this.template = new EJS({element:"template"});
	this.dialog = new EJS({element:"dialog"});
	this.types = {"create":"POST", "update":"PUT", "read":"GET", "delete":"DELETE"};
	
	this.init = function(newkind)
	{
		var curr = this;
		jQuery.ajax({
			url:"/entity/", dataType:"json", type:"GET", data:{},
			success: function(dao){
				curr.kinds = dao;
				if(newkind)
					curr.load(curr.getKind(newkind).index);
				else
					curr.load(curr.selected);
			},
			error: function(e){ console.dir(e); }
		});
	}
	
	this.load = function(k)
	{
		var curr = this;
		var kind = this.kinds.rows[k].id;
		jQuery.ajax({
			url:"/entity/"+kind, dataType:"json", type:"GET", data:{},
			success: function(dao){
				curr.selected = k;
				curr.data = dao;
				var systems = {"id":true,"kind":true};
				for(var i = 0; i < dao.rows.length; i++)
				{
					var row = dao.rows[i];
					var kind = curr.getKind(row.kind);
					kind.columns = {};
					for(var col in row)
						if(!systems[col])
							kind.columns[col] = col;
				}
				curr.render(); 
			},
			error: function(e){ console.dir(e); }
		});
	};
	
	this.getKind = function(name)
	{
		for(var k in this.kinds.rows)
		{
			var kind = this.kinds.rows[k];
			if(kind.id == name)
			{
				kind.index = k;
				return kind;
			}
		}
		return null;
	}
	
	this.execute = function(type, metaidx, idx)
	{
		var url = window.location.href.split("/admin/")[0]+"/entity/";
		var meta = this.kinds.rows[metaidx];
		if(meta)
		{
			url = url + meta.id;
			var inst = this.data.rows[idx];
			if(inst)
				url = url + "/" + inst.id;
		}
		this.action = {type:type, meta:meta, instance:this.data["rows"]?this.data.rows[idx]:null, url:url};
		var thtml = this.dialog.render(this);
		$("#admodal-content").html(thtml);
		$("#admodal").modal("show");
	}
	
	this.save = function(json, newkind)
	{
		var curr = this;
		var dat = JSON.parse(json);
		var url = (typeof newkind=="undefined" || newkind=="")?this.action.url:this.action.url+newkind;
		jQuery.ajax({
			url:url, dataType:"json", type:this.types[this.action.type], data:json,
		    contentType: "application/json; charset=utf-8",
			success: function(dao){
				curr.render();
				if(typeof newkind=="undefined" || newkind=="")
					curr.load(curr.selected);
				else
					curr.init(newkind); 
			},
			error: function(e){ console.dir(e); }
		});
	}
	
	this.render = function()
	{
		var thtml = this.template.render(this);
		jQuery("#content").html(thtml);
	};
};
var explorer = new mExplorer();
explorer.init();
</script>
</html>