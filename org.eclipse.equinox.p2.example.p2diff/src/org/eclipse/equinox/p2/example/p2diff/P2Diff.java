/*******************************************************************************
 *  Copyright (c) 2012 EclipseSource
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     EclipseSource - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.example.p2diff;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * A two way diff tool for p2 metadata repositories.
 * 
 * @author Ian Bull
 *
 */
public class P2Diff {

	private Set<IInstallableUnit> repositoryA;
	private Set<IInstallableUnit> repositoryB;

	/**
	 * Factory method to create a P2Diff tool
	 * @param agent The provisioning agent to use to load the repositories
	 * @param repositoryALocation The location of the first repository
	 * @param repositoryBLocation The location of the second repository
	 * @return A P2Diff tool that can be used to compare the two repositories
	 * @throws ProvisionException Thrown if a repository cannot be read
	 * @throws OperationCanceledException
	 */
	public static P2Diff createP2Diff(IProvisioningAgent agent, URI repositoryALocation, URI repositoryBLocation) throws ProvisionException, OperationCanceledException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		IMetadataRepository repositoryA = manager.loadRepository(repositoryALocation, new NullProgressMonitor());
		IMetadataRepository repositoryB = manager.loadRepository(repositoryBLocation, new NullProgressMonitor());
		return new P2Diff(createAndRunQuery(repositoryA).toUnmodifiableSet(), createAndRunQuery(repositoryB).toUnmodifiableSet());
	}
	
	private static IQueryResult<IInstallableUnit> createAndRunQuery(IQueryable<IInstallableUnit> queryable) {
		IQuery<IInstallableUnit> result = null;
		if (Application.QUERY_TYPE == Application.QueryType.CATEGORIZED ) {
			result = createCompoundCategoryMemberQuery(queryable.query(getCategoryQuery(Application.CATEGORY_NAME), null));
		} else if (Application.QUERY_TYPE == Application.QueryType.GROUPS ) {
			result = QueryUtil.createIUGroupQuery();
		} else {
			result = QueryUtil.createIUAnyQuery();
		}
		if ( Application.ONLY_LATEST ) {
			result = QueryUtil.createLatestQuery(result);
		}
		
		return queryable.query(result, null);
	}
	
	private static IQuery<IInstallableUnit> createCompoundCategoryMemberQuery(IQueryResult<IInstallableUnit> categories) {
		Collection<IQuery<? extends IInstallableUnit>> queries = new ArrayList<IQuery<? extends IInstallableUnit>>();
		for (IInstallableUnit category : categories.toUnmodifiableSet()) {
			queries.add(QueryUtil.createIUCategoryMemberQuery(category));
		}
		return QueryUtil.createCompoundQuery(queries, false);
	}
	
	private static IQuery<IInstallableUnit> getCategoryQuery(String name) {
		if ( name == null ) {
			return QueryUtil.createIUCategoryQuery();
		}
		return QueryUtil.createPipeQuery(QueryUtil.createIUCategoryQuery(), QueryUtil.createMatchQuery("this.translatedProperties[$0] == $1", IInstallableUnit.PROP_NAME, name));
	}

	
	private P2Diff(Set<IInstallableUnit> repositoryA, Set<IInstallableUnit> repositoryB) {
		this.repositoryA = repositoryA;
		this.repositoryB = repositoryB;
	}
	
	/**
	 * Returns the intersection of A and B
	 * @return
	 */
	public Collection<IInstallableUnit> getInersection() {
		HashSet<IInstallableUnit> result = new HashSet<IInstallableUnit>(repositoryA);
		result.addAll(repositoryB);
		return result;
	}
	
	/**
	 * Returns the elements in B that are not in A
	 * @return
	 */
	public IQueryResult<IInstallableUnit> getRelativeComplementA() {
		HashSet<IInstallableUnit> result = new HashSet<IInstallableUnit>(repositoryB);
		result.removeAll(repositoryA);
		return new CollectionResult<IInstallableUnit>(result);
	}
	
	/**
	 * Returns the elements in A that are not in B
	 * @return
	 */
	public IQueryResult<IInstallableUnit> getRelativeComplementB() {
		HashSet<IInstallableUnit> result = new HashSet<IInstallableUnit>(repositoryA);
		result.removeAll(repositoryB);
		return new CollectionResult<IInstallableUnit>(result);
	}

	public IQueryResult<IInstallableUnit> getRepositoryA() {
		return new CollectionResult<>(repositoryA);
	}
	
	public IQueryResult<IInstallableUnit> getRepositoryB() {
		return new CollectionResult<>(repositoryB);
	}
	
}
