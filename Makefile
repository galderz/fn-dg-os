install-templates:
	oc create -f jdg-template.yml || true
	oc replace -f jdg-template.yml || true
.PHONY: install-templates

clear:
	oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts,pvc,pv --selector=template=jdg || true
.PHONY: clear

test:
	oc process jdg | oc create -f -
.PHONY: test